import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.WaitForUserDialog;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/*
 * ImageJ Plugin to open Max projected 4 channel image. DAPI labelled
 * nucleus used to select the nucleus region of the cell. Centromeres
 * are labelled in red (survivin labelled) and Mic region labelled Green.
 * The mean intensities for the green Mic Region were measured and output
 * to text file. The same region was applied to the red (survivin) region 
 * and the region cut. The region is grown by 2 pixels to measure the 
 * intensity immediately outside the red region andbecause the cut region
 *  is black it doesn't feature in the measurement.   
 */

public class Alba_AutoOpen_RegionGrow implements PlugIn{
	String filename;
	ImagePlus BlueImage;
	int BlueImageID;
	ImagePlus GreenImage;
	int GreenImageID;
	ImagePlus RedImage;
	int RedImageID;
	ImagePlus FarRedImage;
	int FarRedImageID;
	int RoiIndexPos;
	double BkGrdGreen;
	double BkGrdRed;
	
	public void run(String arg) {
		
		//Set measurements used in plugin
		IJ.run("Set Measurements...", "area mean min centroid integrated redirect=None decimal=2");
		new WaitForUserDialog("Open Image", "Open Images. SPLIT CHANNELS!").show();
		IJ.run("Bio-Formats Importer"); //Open images using Bio-Formats
		
		ImagePlus imp = WindowManager.getCurrentImage();
		filename = imp.getShortTitle();//Get file name
		String dirstr = "C:\\Temp\\"; 
		
		/*
		 * Populate the window ID list to select each colour channel.
		 * For the plugin to work the images must have been acquired
		 * Green, Red, Far-Red and Blue. The plugin will fail if this
		 * order isn't followed.
		 */
		int [] windowList = WindowManager.getIDList();
		//Blue "DAPI" channel
		IJ.selectWindow(windowList[3]);
		BlueImage = WindowManager.getCurrentImage();
		BlueImageID = BlueImage.getID();
		IJ.run(BlueImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
		//Green "Mic labelled" channel
		IJ.selectWindow(windowList[0]);
		GreenImage = WindowManager.getCurrentImage();
		GreenImageID = GreenImage.getID();
		IJ.run(GreenImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
		//Red survivin channel
		IJ.selectWindow(windowList[1]);
		RedImage = WindowManager.getCurrentImage();
		RedImageID = RedImage.getID();
		IJ.run(RedImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
		//Far-Red channel
		IJ.selectWindow(windowList[2]);
		FarRedImage = WindowManager.getCurrentImage();
		FarRedImageID = FarRedImage.getID();
		IJ.run(FarRedImage, "Enhance Contrast", "saturated=0.35"); //Autoscale image
		
		selectBackround(); //Method for background measurement in green and red channels
		
		RoiManager PickOne = new RoiManager(); 
		PickOne = SelectReferenceCells(); //Method to select the cell
		CountFarRed(PickOne);	//Quantify the selected cell
		IJ.run("Close All", "");
		new WaitForUserDialog("Finished" , "The plugin has finished").show();
	}
	
	private void selectBackround(){
		/*
		 * Method to place an oval region in the non
		 * cell regions of the green and red channels.
		 * The values are added to the text file output 
		 * at the end.
		 */
		IJ.setTool("oval");
		for(int b=0;b<2;b++){
			if (b==0){		
				IJ.selectWindow(GreenImageID);
				new WaitForUserDialog("BkGrd" , "Draw Background Oval").show();
				IJ.setThreshold(GreenImage, 0, 65535);
				IJ.run(GreenImage, "Measure", "");
				ResultsTable rt = new ResultsTable();	
				rt = Analyzer.getResultsTable();
				BkGrdGreen = rt.getValueAsDouble(1, 0);
				IJ.deleteRows(0, 0);
			}
			if (b==1){
				IJ.selectWindow(RedImageID);
				new WaitForUserDialog("BkGrd" , "Draw Background Oval").show();
				IJ.setThreshold(RedImage, 0, 65535);
				IJ.run(RedImage, "Measure", "");
				ResultsTable rt = new ResultsTable();	
				rt = Analyzer.getResultsTable();
				BkGrdRed = rt.getValueAsDouble(1, 0);
				IJ.deleteRows(0, 0);
			}
		}

	}
	
	private RoiManager SelectReferenceCells(){
		
		//Check ROI Manager is empty before starting
		RoiManager TestEmpty = RoiManager.getInstance();
		int EmptyROI = TestEmpty.getCount();
		if (EmptyROI>0){
			TestEmpty.runCommand(BlueImage,"Deselect");
			TestEmpty.runCommand(BlueImage,"Delete");
		}
		/*
		 * Select the correct cell from the ROI manager. Its
		 * based on the DAPI labelling which needs to be well
		 * defined to ensure the maximum number of centromeres
		 * are selected.
		 */
		IJ.selectWindow(BlueImageID);
		ImagePlus imp = null;
		IJ.setAutoThreshold(BlueImage, "Default dark");
		IJ.run("Threshold...");
		new WaitForUserDialog("Adjust" , "Adjust nucleus threshold if required").show();
		IJ.run(BlueImage, "Analyze Particles...", "size=150-Infinity pixel exclude include add");
		RoiManager PickOne = RoiManager.getInstance();
		int RoiCount = PickOne.getCount();
		RoiIndexPos = 0;
		if(RoiCount>1){
			new WaitForUserDialog("Select" , "Select cell of interest from ROI Manager").show();
			RoiIndexPos = PickOne.getSelectedIndex();
		}
		
		return PickOne;
	
	}
	
	private void CountFarRed(RoiManager PickOne){
		
		/*
		 * Method uses the Far-Red labelled AcA to accurately
		 * identify the centromeres and put their ROI into the
		 * ROI manager. The ROI's are then applied to both the 
		 * green "MiC" channel and red "survivin" channel. The
		 * ROI is cut to render it pure black on the image, the 
		 * ROI is grown by 2 pixels and the mean intensity is
		 * measured in this 2 pixel wide region around the 
		 * centromere.  
		 */
		IJ.selectWindow(FarRedImageID);
		
		IJ.run(FarRedImage, "Unsharp Mask...", "radius=1 mask=0.80");
		PickOne.select(RoiIndexPos);
		IJ.setAutoThreshold(FarRedImage, "Default dark");
		IJ.run(FarRedImage, "Analyze Particles...", "size=0.02-3.00 show=Masks");
		ImagePlus MaskImage = WindowManager.getCurrentImage();
		int MaskImageID = MaskImage.getID();
		IJ.selectWindow(FarRedImageID);
		PickOne.runCommand(FarRedImage,"Deselect");
		PickOne.runCommand(FarRedImage,"Delete");
		IJ.selectWindow(MaskImageID);
		IJ.setAutoThreshold(MaskImage, "Default");
		IJ.run(MaskImage, "Analyze Particles...", "size=0.02-3.00 add");
		
		//Make sure Results Table is empty before start
		ResultsTable emptyrt = new ResultsTable();	
		emptyrt = Analyzer.getResultsTable();
		int valnums = emptyrt.getCounter();
		for(int a=0;a<valnums;a++){
			IJ.deleteRows(0, a);
		}
		
		
		RoiManager rm = RoiManager.getInstance();
		int NumRoi = rm.getCount();
		
		IJ.selectWindow(RedImageID);
		Calibration xval = RedImage.getCalibration();
		ImageProcessor RedProcess = RedImage.getProcessor();
		double ThreshMax = RedProcess.getMax();
		
		//Measure ROI ring in red channel
		for (int x=0;x<NumRoi;x++){
			rm.select(x);
			IJ.run(RedImage, "Cut", "");
			IJ.run(RedImage, "Enlarge...", "enlarge=2 pixel");
			IJ.setThreshold(RedImage, 3, 65535);
			IJ.run(RedImage, "Analyze Particles...", "size=0.02-3.00 display");
		}
		double MeanInt[] = GetMeasurements(NumRoi);
		String colour = "Red";
		OutputText(MeanInt,colour); //Output results to text file
		
		//Clear Results Table
		ResultsTable ClearRT = new ResultsTable();	
		ClearRT = Analyzer.getResultsTable();
		int numres = ClearRT.getCounter();
		if (numres > 0){
			IJ.deleteRows(0, numres);
		}
		
		//Measure ROI ring in green channel
		IJ.selectWindow(GreenImageID);
		for (int x=0;x<NumRoi;x++){
			rm.select(x);
			IJ.run(GreenImage, "Cut", "");
			IJ.run(GreenImage, "Enlarge...", "enlarge=2 pixel");
			IJ.setThreshold(GreenImage, 3, 65535);
			IJ.run(GreenImage, "Analyze Particles...", "size=0.02-3.00 display");
		}
		double MeanInt2[] = GetMeasurements(NumRoi);
		MeanInt = MeanInt2;
		colour = "Green";
		OutputText(MeanInt,colour); //Output green channel results to text file 
		
	}

	private double[] GetMeasurements(int NumRoi){
		
		/*
		 * Method gets the intensity measurements from the 
		 * results table for each channel and stores them
		 * in an area to be passed to the outputtext method.
		 */
		double MeanInt[] = new double[NumRoi];
		ResultsTable rt = new ResultsTable();	
		rt = Analyzer.getResultsTable();
		int valnums = rt.getCounter();
		
		for(int y=0;y<valnums;y++){
			MeanInt[y] = rt.getValueAsDouble(1, y);
		}
		
		return MeanInt;
	}
	
	private void OutputText(double [] MeanInt, String colour){
		
		/*
		 * Method formats the intensity data into a text file 
		 * for import into Excel, R or Graphpad
		 */
		String CreateName = "C:/Temp/Results.txt";
		String FILE_NAME = CreateName;
    	
		try{
			FileWriter fileWriter = new FileWriter(FILE_NAME,true);
			BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
			
			if(colour.equals("Red")){
				bufferedWriter.newLine();
				bufferedWriter.newLine();
				bufferedWriter.write(" File= " + filename);
				bufferedWriter.newLine();
				bufferedWriter.write(" Green Background = " + BkGrdGreen);
				bufferedWriter.newLine();
				bufferedWriter.write(" Red Background = " + BkGrdRed);
				bufferedWriter.newLine();
				bufferedWriter.newLine();
			}
			
			int numvals = MeanInt.length;
			for (int z = 0; z<numvals;z++){
				bufferedWriter.write(colour + " Dot = " + z + " Mean Intensity = " + MeanInt[z]);
				bufferedWriter.newLine();
			}
			
			bufferedWriter.close();

		}
		catch(IOException ex) {
            System.out.println(
                "Error writing to file '"
                + FILE_NAME + "'");
        }
	}
}
