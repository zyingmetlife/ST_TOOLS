//How to use this program
//1. Need to use jdk 1.6
//2. the starteam93.jar on the classpath
//3. command-line arguments: 
//      ST_ServerName, ST_Server_Port ST_User_ID, ST_User_Passwd, ST_Folder_Name, 
//      Local_Working_Dir, FileName, Comments, Reason
// The Project name BFST_AdCfg is hard-coded in this program to avoid mistakenly update other projects
//Example:
// FileCheckIn as-riscstartm02 49201 bf7admin ****** "INST" "TestFile.txt" "c:\\CheckinTest\\" "Test.txt" "TestCheckIn" "Reason"

package com.metlife.dcm.sttools;

import java.io.*;

import com.starbase.starteam.*;
import com.starbase.util.Platform;

class FileCheckOutByDate_CheckinByLabel {
	String projectName="BFST_AdCfg"; //hard-code the project name to avoid damage to other projects
	String workingFolderPath="";  //note: must be in the format of D:\\name\\subname\\
	String workingFilename=null;
	String stFoldername=null;
	String comment="comment";
	String reason="reason";
	Project proj;
	int projectID=0;
	boolean m_bVerbose = true; // true for verbose output
	boolean m_bRecursive = false; //not recursively search the sub-folders
	
public static void main(String args[]){
	FileCheckOutByDate_CheckinByLabel app = new FileCheckOutByDate_CheckinByLabel();
	String id=args[2];
	String password=args[3];
	String serverName=args[0];
	int port=Integer.parseInt(args[1]);
	app.stFoldername=args[4];	//the LOB folder name under BFST_AdCfg
	app.workingFolderPath=args[5];  //note: must be in the format of D:\\name\\subname\\
	app.workingFilename=args[6];
	String workingFilePath=app.workingFolderPath+"\\"+app.workingFilename;
	app.comment=args[7];
	app.reason=args[8];
	String tmp=null;
	app.proj= null;
	
	
	if ( args.length <  9 ) {
  	    System.out.println("missing arguments");
   	    System.out.println("Usage:");
   	    System.out.println("java com.metlife.dcm.FileCheckIn.class st_svr st_port st_id st_passwd st_folder local_path filename comments reason");
     }	    
    else {
   	  // for (int i = 0; i < args.length; i++)
      //     System.out.println(args[i]);
    
		Server server=new Server(serverName,port);
		System.out.println("connecting to server:"+serverName);
		server.connect();
		System.out.println(serverName+" Connected");
		server.logOn(id,password);
		System.out.println("user "+id +" Logged in to server "+ serverName+" port "+port);
	
		Project[] projects = server.getProjects();
		View[] views=null;
		for(int i=0;i<projects.length;i++)
			{
			app.proj = projects[i];
			tmp=app.proj.getName();
			if(tmp.equals(app.projectName))
				{
					app.projectID=app.proj.getID();
					views=app.proj.getViews();  //views will hold the current projects' views
					System.out.println("Found the project->"+app.projectName);
					System.out.println("ProjectID->"+app.projectID);
					break;
				}
			}
	
		Folder rootFolder=views[0].getRootFolder();
		Folder stFolder=StarTeamFinder.findFolder(rootFolder,app.stFoldername);
	
		Type tp = server.typeForName("File"); //get the type object
		
		 // We always display the ItemID (OBJECT_ID).
	    int nProperties = 1;
	
	    // Does this item type have a primary descriptor?
	    // If so, we'll need it.
	    Property p1 = app.getPrimaryDescriptor(tp);
	    if (p1 != null) {
	        nProperties++;
	    }
	
	    // Does this item type have a secondary descriptor?
	    // If so, we'll need it.
	    Property p2 = app.getSecondaryDescriptor(tp);
	    if (p2 != null) {
	        nProperties++;
	    }
	    
	    // Does this item type have a CreatedTime?
	    // If so, we'll need it.
	    Property p3 = app.getCreationTime(tp);
	    if (p2 != null) {
	        nProperties++;
	    }
	
	    // Now, build an array of the property names.
	    String[] strNames = new String[nProperties];
	    int iProperty = 0;
	    strNames[iProperty++] = server.getPropertyNames().OBJECT_ID;
	   
	    if (p1 != null) {
	        strNames[iProperty++] = p1.getName();
	    }
	    if (p2 != null) {
	        strNames[iProperty++] = p2.getName();
	    }
	    if (p3 != null) {
	        strNames[iProperty++] = p3.getName();
	    }
	
	    // Pre-fetch the item properties and cache them.
	    stFolder.populateNow(tp.getName(), strNames, -1);
	
	    // Try to find the ByDate CONFIGSPEC in the dev view
	    com.starbase.starteam.File existingTipFile = app.findFile(server, app.proj, views[0], tp, stFolder, app.workingFilename);
	    if (existingTipFile == null) {  //could not find the file with the input name
	    	Utilities.log("Could not find the file with the input file name:"+app.workingFilename,Utilities.DEBUG_LEVEL_CRITICAL);
	    	Utilities.terminate();
	    }
	    else {
	    	 // try to find the byLabelTipFile
	    	 String byLabelWorkingFilename = app.workingFilename.substring(0,app.workingFilename.length()-4)+"_ByLabel.txt";
	    	 com.starbase.starteam.File findTipByLabelFile = app.findFile(server, app.proj, views[0], tp, stFolder, byLabelWorkingFilename);
	    	
	    	 if (findTipByLabelFile == null) { //case where the ByLabel tip file does not exist
	    	     	 String byLabelWorkingFilePath = workingFilePath.substring(0,workingFilePath.length()-4)+"_ByLabel.txt";
	    	     	 Utilities.log("ByDate filename="+workingFilePath+"  ByLabel filename="+byLabelWorkingFilePath,Utilities.DEBUG_LEVEL_LOW); 
	    	     	 try {
	    	     		 	//create the checkoutOptions from the view
	    	     		    CheckoutOptions coOpts = new CheckoutOptions(views[0]);
	    	     		    coOpts.setTimeStampNow(false);
	    	     		    // Create a CheckoutManager from a View with default options.
	    	     		 	CheckoutManager coMgr = new CheckoutManager(views[0],coOpts);

	    	     		 	// Check-out a single file to its default location from an auto-located
	    	     		 	// MPX Cache Agent.
	    	     		 	coMgr.setMPXCacheAgentEnabled(true);   // default is auto-locate
	    	     		 	com.starbase.starteam.File file = StarTeamFinder.findFile(stFolder, app.workingFilename, false);
	    		 
	    	     		 	//create the working file based on the input path and drive, checkout the StarTeam file to the working file location
	    	     		 	java.io.File workingFile = new java.io.File(workingFilePath);
	    	     		 	coMgr.checkoutTo(file,workingFile);

	    	     		 	app.replaceString("CURDATE","LABEL",workingFilePath,byLabelWorkingFilePath);      
	    	     	 }
	    	     	 catch (Exception e) {
	    	     		 Utilities.log(e.toString(),Utilities.DEBUG_LEVEL_CRITICAL);
	    	     		 Utilities.log("Failed to checkout the existing file",Utilities.DEBUG_LEVEL_CRITICAL);
	    	     		 Utilities.terminate();
	    	     	 }
	    	 
	    	     	 //now we have created the ByLabel file, we need to check it in
	    	     	 try{
	    	     		 java.io.File byLabelWorkingFile = new java.io.File(byLabelWorkingFilePath);
	    	     		 com.starbase.starteam.File stFile=new com.starbase.starteam.File(stFolder);   
	    	     		 stFile.add(byLabelWorkingFile,byLabelWorkingFilename,app.comment,app.reason,0,false,true);
	    	     		 System.out.println(byLabelWorkingFile.getPath() +" is added to ST project BFST_AdCfg's "+app.stFoldername+" folder");
	    	     	 }
	    	     	 catch (Exception e) {
	    	     		 e.printStackTrace();
			   	     }
	    	     	 finally {		
	    	     		 // Free up the memory used by the cached items.
	    	     		 stFolder.discardItems(tp.getName(), -1);
	    	     	 }
	    	 }
	    	 else {
	    		 Utilities.log("The Bylabel CONFIGSOPEC file:"+byLabelWorkingFilename+" already exists in StarTeam "+app.projectName+"\\"+app.stFoldername+", please manually check",Utilities.DEBUG_LEVEL_CRITICAL);
	    		 Utilities.terminate();
	    	 }
	    }
    }
}

//---------------------------- to be worked on
com.starbase.starteam.File findFile(Server s, Project p, View v, Type t, Folder f, String fname) {
    String strFolder = f.getFolderHierarchy();
    String strSlash = Platform.getFilePathDelim();
    com.starbase.starteam.File rt = null;  //initialize the return file handle to null
    int i = strFolder.indexOf(strSlash);
    if (i >= 0) {
        strFolder = strFolder.substring(i + 1);
    }
    // For the root folder, display just the slash.
    if (strFolder.equals("")) {
        strFolder = strSlash;
    }
    System.out.println("subfolder from root: \"" + strFolder + "\"");
 
    // Process all items in this folder.
    Item[] items = f.getItems(t.getName());
    for (i = 0; i < items.length; i++) {
    	// Get descriptors for this item type.
        Property p1 = getPrimaryDescriptor(t);
        Property p2 = getSecondaryDescriptor(t);
        Property p3 = getCreationTime(t);
        // Show the primary descriptor, if there is one.
        // There should always be one.
        String temp_filename= null;
        String temp_description=null;
        String temp_time=null;
        
        if (p1 != null) {
             temp_filename = formatForDisplay(p1, items[i].get(p1.getName()));
        }
        // Show the secondary descriptor, if there is one.
        // Some item types have one, some don't.
        if (p2 != null) {
            temp_description= formatForDisplay(p2, items[i].get(p2.getName()));
          
        }
        if (p3 != null) {
            temp_time= formatForDisplayTime(p3, items[i].get(p3.getName()));
        }
        if (temp_filename.equals(fname)) {
                  System.out.println ("file "+temp_filename+" exists!    Description=" + temp_description.replaceAll("(\\r|\\n)", "") + "\t Created on " + temp_time);
                  rt = (com.starbase.starteam.File) items[i];
                  break; //found the file, exit for loop
        }
     }
     return rt;
}



//----------------------------------------------------------------------------
// Get the primary descriptor of the given item type.
// Returns null if there isn't one.
// In practice, all item types have a primary descriptor.
// ----------------------------------------------------------------------------
protected Property getPrimaryDescriptor(Type t) {
    Property[] properties = t.getProperties();
    for (int i = 0; i < properties.length; i++) {
        Property p = properties[i];
        if (p.isPrimaryDescriptor()) {
            return (p);
        }
    }
    return (null);
}

// ----------------------------------------------------------------------------
// Get the secondary descriptor of the given item type.
// Returns null if there isn't one.
// ----------------------------------------------------------------------------
protected Property getSecondaryDescriptor(Type t) {
    Property[] properties = t.getProperties();
    for (int i = 0; i < properties.length; i++) {
         Property p = properties[i];
         if (p.isDescriptor() && !p.isPrimaryDescriptor()) {
             return (p);
         }
     }
     return (null);
}

protected Property getCreationTime(Type t) {
    Property[] properties = t.getProperties();
    for (int i = 0; i < properties.length; i++) {
         Property p = properties[i];
         String tStr =p.toString();
         if ( tStr.equals("CreatedTime")) {
        	  return (p);
        }
     }
     return (null);
}
    
// ----------------------------------------------------------------------------
// Formats a property value for display to the user.
// ----------------------------------------------------------------------------
protected String formatForDisplay(Property p, Object value) {
        String str = p.getDisplayValue(value);
        if (p.getTypeCode() == Property.Types.TEXT) {
           return (str);
        } else {
            return str;
        }
    }    

protected String formatForDisplayTime(Property p, Object value) {
    String str = p.getDisplayValue(value);
    if (p.getTypeCode() == Property.Types.TIME) {
       return (str);
    } else {
        return str;
    }
}    

protected void replaceString(String pattern, String replacement, String inFilePathName, String outFilePathName)
{
	try {
	    java.io.File ifile = new java.io.File(inFilePathName);
	    BufferedReader reader = new BufferedReader(new FileReader(ifile));
	    String line = "", oldtext = "";
	    while((line = reader.readLine()) != null)
	        {
	         oldtext += line + "\r\n";
	    }
	    reader.close();
	    // replace a word in a file
	    String newtext = oldtext.replaceAll(pattern, replacement);
	   
	    FileWriter writer = new FileWriter(outFilePathName);
	    writer.write(newtext);
	    writer.close();
    }
	catch (IOException ioe) {
		ioe.printStackTrace();
	}
}

}
