/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  PdeSketch - stores information about files in the current sketch
  Part of the Processing project - http://processing.org

  Except where noted, code is written by Ben Fry
  Copyright (c) 2001-03 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License 
  along with this program; if not, write to the Free Software Foundation, 
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

public class PdeSketch {
  // name of sketch, which is the name of main file 
  // (without .pde or .java extension)
  String name;  

  // 
  String path;  // path to 'main' file for this sketch

  // the sketch folder
  File directory;

  static final int PDE = 0;
  static final int JAVA = 1; 

  PdeCode current;
  int codeCount;
  PdeCode code[];

  int hiddenCount;
  PdeCode hidden[];


  /**
   * path is location of the main .pde file, because this is also
   * simplest to use when opening the file from the finder/explorer.
   */
  public PdeSketch(String path) throws IOException {
    File mainFile = new File(path);
    System.out.println("main file is " + mainFile);

    main = mainFile.getName();
    System.out.println("main file is " + main);
    /*
    if (main.endsWith(".pde")) {
      main = main.substring(0, main.length() - 4);

    } else if (main.endsWith(".java")) {
      main = main.substring(0, main.length() - 5);
    }
    */

    directory = new File(path.getParent());
    System.out.println("sketch dir is " + directory);

    load();
  }


  /**
   * Build the list of files. 
   *
   * Generally this is only done once, rather than
   * each time a change is made, because otherwise it gets to be 
   * a nightmare to keep track of what files went where, because 
   * not all the data will be saved to disk.
   *
   * The exception is when an external editor is in use,
   * in which case the load happens each time "run" is hit.
   */
  public void load() {
    // get list of files in the sketch folder
    String list[] = directory.list();

    for (int i = 0; i < list.length; i++) {
      if (list[i].endsWith(".pde")) fileCount++;
      else if (list[i].endsWith(".java")) fileCount++;
      else if (list[i].endsWith(".pde.x")) hiddenCount++;
      else if (list[i].endsWith(".java.x")) hiddenCount++;
    }

    code = new PdeCode[codeCount];
    hidden = new PdeCode[hiddenCount];

    int fileCounter = 0;
    int hiddenCounter = 0;

    for (int i = 0; i < list.length; i++) {
      if (list[i].endsWith(".pde")) {
        code[fileCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 4), 
                      new File(directory, list[i]), 
                      PDE);

      } else if (list[i].endsWith(".java")) {
        code[fileCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 5),
                      new File(directory, list[i]),
                      JAVA);

      } else if (list[i].endsWith(".pde.x")) {
        hidden[hiddenCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 6),
                      new File(directory, list[i]),
                      PDE);

      } else if (list[i].endsWith(".java.x")) {
        hidden[hiddenCounter++] = 
          new PdeCode(list[i].substring(0, list[i].length() - 7),
                      new File(directory, list[i]),
                      JAVA);
      }      
    }

    // remove any entries that didn't load properly
    int index = 0;
    while (index < codeCount) {
      if (code[index].program == null) {
        //hide(index);  // although will this file be hidable?
        for (int i = index+1; i < codeCount; i++) {
          code[i-1] = code[i];
        }
        codeCount--;

      } else {
        index++;
      }
    }

    // move the main class to the first tab
    // start at 1, if it's at zero, don't bother
    for (int i = 1; i < codeCount; i++) {
      if (code[i].file.getName().equals("main")) {
        System.out.println("found main code at slot " + i);
        PdeCode temp = code[0];
        code[0] = code[i];
        code[i] = temp;
      }
    }

    // cheap-ass sort of the rest of the files
    // it's a dumb, slow sort, but there shouldn't be more than ~5 files
    for (int i = 1; i < codeCount; i++) {
      int who = i;
      //String who = code[i].name;
      //int whoIndex = i;
      for (int j = i + 1; j < codeCount; j++) {
        if (code[j].name.compare(code[who].name) < 0) {
          who = j;  // this guy is earlier in the alphabet
        }
      }
      if (who != i) {  // swap with someone if changes made
        PdeCode temp = code[who];
        code[who] = code[i];
        code[i] = temp;
      }
    }
  }


  /**
   * Change what file is currently being edited. 
   * 1. store the String for the text of the current file.
   * 2. retrieve the String for the text of the new file.
   * 3. change the text that's visible in the text area
   */
  public void setCurrent(int which) {
    // get the text currently being edited
    //program[current] = editor.getText();
    if (current != null) {
      current.program = editor.getText();
    }

    current = code[which];

    // set to the text for this file
    // 'true' means to wipe out the undo buffer
    // (so they don't undo back to the other file.. whups!)
    editor.changeText(current.program, true); 

    // and i'll personally make a note of the change
    //current = which;
  }



  /**
   * This is not Runnable.run(), but a handler for the run() command.
   *
   * run externally if a code folder exists, 
   * or if more than one file is in the project
   *
   */
  public void run() {
    try {
      current.program = textarea.getText();

      // TODO record history here
      //current.history.record(program, PdeHistory.RUN);

      // if an external editor is being used, need to grab the
      // latest version of the code from the file.
      if (PdePreferences.getBoolean("editor.external")) {
        // history gets screwed by the open..
        //String historySaved = history.lastRecorded;
        //handleOpen(sketch);
        //history.lastRecorded = historySaved;

        // nuke previous files and settings, just get things loaded
        load();
      }

      // temporary build folder is inside 'lib'
      // this is added to the classpath by default
      tempBuildPath = "lib" + File.separator + "build";
      File buildDir = new File(tempBuildPath);
      if (!buildDir.exists()) {
        buildDir.mkdirs();
      }

      // copy contents of data dir into lib/build
      // TODO write a file sync procedure here.. if the files 
      //      already exist in the target, or haven't been modified
      //      don't' bother. this can waste a lot of time when running.
      File dataDir = new File(directory, "data");
      if (dataDir.exists()) {
        // just drop the files in the build folder (pre-68)
        //PdeBase.copyDir(dataDir, buildDir);
        // drop the files into a 'data' subfolder of the build dir
        PdeBase.copyDir(dataDir, new File(buildDir, "data"));
      }

      // start with the main 

      // make up a temporary class name to suggest
      // only used if the code is not in ADVANCED mode
      String suggestedClassName = 
        ("Temporary_" + String.valueOf((int) (Math.random() * 10000)) +
         "_" + String.valueOf((int) (Math.random() * 10000)));

      // handle preprocessing the main file's code
      String mainClassName = build(tempBuildPath, suggestedClassName);
      // externalPaths is magically set by build()

      // if the compilation worked, run the applet
      if (mainClassName != null) {

        if (externalPaths == null) {
          externalPaths = 
            PdeCompiler.calcClassPath(null) + File.pathSeparator + 
            tempBuildPath;
        } else {
          externalPaths = 
            tempBuildPath + File.pathSeparator +
            PdeCompiler.calcClassPath(null) + File.pathSeparator +
            externalPaths;
        }

        // get a useful folder name for the 'code' folder
        // so that it can be included in the java.library.path
        String codeFolderPath = "";
        if (externalCode != null) {
          codeFolderPath = externalCode.getCanonicalPath();
        }

        // create a runtime object
        runtime = new PdeRuntime(this, className,
                                 externalRuntime, 
                                 codeFolderPath, externalPaths);

        // if programType is ADVANCED
        //   or the code/ folder is not empty -> or just exists (simpler)
        // then set boolean for external to true
        // include path to build in front, then path for code folder
        //   when passing the classpath through
        //   actually, build will already be in there, just prepend code

        // use the runtime object to consume the errors now
        //messageStream.setMessageConsumer(runtime);
        // no need to bother recycling the old guy
        PdeMessageStream messageStream = new PdeMessageStream(runtime);

        // start the applet
        runtime.start(presenting ? presentLocation : appletLocation,
                         new PrintStream(messageStream));
                         //leechErr);

        // spawn a thread to update PDE GUI state
        watcher = new RunButtonWatcher();

      } else {
        // [dmose] throw an exception here?
        // [fry] iirc the exception will have already been thrown
        cleanTempFiles(); //tempBuildPath);
      }
    } catch (PdeException e) { 
      // if it made it as far as creating a Runtime object, 
      // call its stop method to unwind its thread
      if (runtime != null) runtime.stop();
      cleanTempFiles(); //tempBuildPath);

      // printing the stack trace may be overkill since it happens
      // even on a simple parse error
      //e.printStackTrace();

      error(e);

    } catch (Exception e) {  // something more general happened
      e.printStackTrace();

      // if it made it as far as creating a Runtime object, 
      // call its stop method to unwind its thread
      if (runtime != null) runtime.stop();

      cleanTempFiles(); //tempBuildPath);
    } 
  }


  /**
   * Have the contents of the currently visible tab been modified.
   */
  /*
  public boolean isCurrentModified() {
    return modified[current];
  }
  */


  /**
   * Build all the code for this sketch.
   *
   * In an advanced program, the returned classname could be different,
   * which is why the className is set based on the return value.
   * @return null if compilation failed, className if not
   */
  protected String build(String buildPath, String suggestedClassName)
    throws PdeException, Exception {

    boolean externalRuntime = false;
    //externalPaths = null;
    String additionalImports[] = null;
    String additionalClassPath = null;

    // figure out the contents of the code folder to see if there
    // are files that need to be added to the imports
    File codeFolder = new File(sketchDir, "code");
    if (codeFolder.exists()) {
      externalRuntime = true;
      additionalClassPath = PdeCompiler.contentsToClassPath(codeFolder);
      additionalImports = PdeCompiler.magicImports(additionalClassPath);
    } else {
      codeFolder = null;
    }

    // first run preproc on the 'main' file, using the sugg class name
    // then for code 1..count
    //   if .java, write programs[i] to buildpath
    //   if .pde, run preproc to buildpath
    //     if no class def'd for the pde file, then complain

    PdePreprocessor preprocessor = new PdePreprocessor();
    try {
      mainClassName = 
        preprocessor.write(program, buildPath,
                           suggestedClassName, externalImports);

    } catch (antlr.RecognitionException re) {
      // this even returns a column
      throw new PdeException(re.getMessage(), 
                             re.getLine() - 1, re.getColumn());

    } catch (antlr.TokenStreamRecognitionException tsre) {
      // while this seems to store line and column internally,
      // there doesn't seem to be a method to grab it.. 
      // so instead it's done using a regexp

      PatternMatcher matcher = new Perl5Matcher();
      PatternCompiler compiler = new Perl5Compiler();
      // line 3:1: unexpected char: 0xA0
      String mess = "^line (\\d+):(\\d+):\\s";
      Pattern pattern = compiler.compile(mess);

      PatternMatcherInput input = 
        new PatternMatcherInput(tsre.toString());
      if (matcher.contains(input, pattern)) {
        MatchResult result = matcher.getMatch();

        int line = Integer.parseInt(result.group(1).toString());
        int column = Integer.parseInt(result.group(2).toString());
        throw new PdeException(tsre.getMessage(), line-1, column);

      } else {
        throw new PdeException(tsre.toString());
      }

    } catch (PdeException pe) {
      throw pe;

    } catch (Exception ex) {
      System.err.println("Uncaught exception type:" + ex.getClass());
      ex.printStackTrace();
      throw new PdeException(ex.toString());
    }

    if (PdePreprocessor.programType == PdePreprocessor.JAVA) {
      externalRuntime = true; // we in advanced mode now, boy
    }
    if (codeCount > 1) {
      externalRuntime = true;
    }

    // compile the program
    //
    PdeCompiler compiler = 
      new PdeCompiler(buildPath, mainClassName, externalCode, this);

    // run the compiler, and funnel errors to the leechErr
    // which is a wrapped around 
    // (this will catch and parse errors during compilation
    // the messageStream will call message() for 'compiler')
    messageStream = new PdeMessageStream(compiler);
    boolean success = compiler.compile(new PrintStream(messageStream));

    return success ? className : null;
  }


  /**
   * Called by PdeEditor to handle someone having selected 'export'. 
   * Pops up a dialog box for export options, and then calls the
   * necessary function with the parameters from the window.
   *
   *
   * +-------------------------------------------------------+
   * +                                                       +
   * + Export to:  [ Applet (for the web)   + ]    [  OK  ]  +
   * +                                                       +
   * + [ ] OK to overwrite HTML file   <-- only visible if there is one there
   * +                                     remembers previous setting as a pref
   * + > Advanced                                            +
   * +                                                       +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Version: [ Java 1.1   + ]                           +
   * +                                                       +
   * +   Recommended version of Java when exporting applets. +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Version: [ Java 1.3   + ]                           +
   * +                                                       +
   * +   Java 1.3 is not recommended for applets,            +
   * +   unless you are using features that require it.      +
   * +   Using a version of Java other than 1.1 will require +
   * +   your Windows users to install the Java Plug-In,     +
   * +   and your Macintosh users to be running OS X.        +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Version: [ Java 1.4   + ]                           +
   * +                                                       +
   * +   identical message as 1.3 above...                   +
   * +                                                       +
   * +-------------------------------------------------------+
   * 
   * +-------------------------------------------------------+
   * +                                                       +
   * + Export to:  [ Application            + ]    [  OK  ]  + 
   * +                                                       +
   * + > Advanced                                            +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Version: [ Java 1.1   + ]                           +
   * +                                                       +
   * +   Not much point to using Java 1.1 for applications.  +
   * +   To run applications, all users will have to         + 
   * +   install Java, in which case they'll most likely     +
   * +   have version 1.3 or later.                          +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Version: [ Java 1.3   + ]                           +
   * +                                                       +
   * +   Java 1.3 is the recommended setting for exporting   +
   * +   applications. Applications will run on any Windows  +
   * +   or Unix machine with Java installed. Mac OS X has   +
   * +   Java installed with the operation system, so there  +
   * +   is no additional installation will be required.     +
   * +                                                       +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +                                                       +
   * +   Platform: [ Mac OS X   + ]    <-- defaults to current platform
   * +                                                       +
   * +   Exports the application as a double-clickable       + 
   * +   .app package, compatible with Mac OS X.             +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Platform: [ Windows    + ]                          +
   * +                                                       +
   * +   Exports the application as a double-clickable       +
   * +   .exe and a handful of supporting files.             +
   * + - - - - - - - - - - - - - - - - - - - - - - - - - - - +
   * +   Platform: [ jar file   + ]                          +
   * +                                                       +
   * +   A jar file can be used on any platform that has     +
   * +   Java installed. Simply doube-click the jar (or type +
   * +   "java -jar sketch.jar" at a command prompt) to run  +
   * +   the application. It is the least fancy method for   +
   * +   exporting.                                          +
   * +                                                       +
   * +-------------------------------------------------------+
   *

   * +-------------------------------------------------------+
   * +                                                       +
   * + Export to:  [ Library                + ]    [  OK  ]  +
   * +                                                       +
   * +-------------------------------------------------------+
   */
  public boolean export() throws Exception {
    return exportApplet(true);
  }


  public boolean exportApplet(boolean replaceHtml) throws Exception {
    //File appletDir, String exportSketchName, File dataDir) {
    //String program = textarea.getText();

    // create the project directory
    // pass null for datapath because the files shouldn't be 
    // copied to the build dir.. that's only for the temp stuff
    File appletDir = new File(directory, "applet");

    boolean writeHtml = true;
    if (appletDir.exists()) {
      File htmlFile = new new File(appletDir, "index.html");
      if (htmlFile.exists() && !replaceHtml) {
        writeHtml = false;
      }
    } else {
      appletDir.mkdirs();
    }

    // build the sketch 
    String foundName = build(name, appletDir.getPath());

    // (already reported) error during export, exit this function
    if (foundName == null) return false;

    // if name != exportSketchName, then that's weirdness
    // BUG unfortunately, that can also be a bug in the preproc :(
    if (!name.equals(foundName)) {
      PdeBase.showWarning("Error during export", 
                          "Sketch name is " + name + " but the sketch\n" +
                          "name in the code was " + foundName);
      return false;
    }

    if (writeHtml) {
      int wide = BApplet.DEFAULT_WIDTH;
      int high = BApplet.DEFAULT_HEIGHT;

      //try {
      PatternMatcher matcher = new Perl5Matcher();
      PatternCompiler compiler = new Perl5Compiler();

      // this matches against any uses of the size() function, 
      // whether they contain numbers of variables or whatever. 
      // this way, no warning is shown if size() isn't actually 
      // used in the applet, which is the case especially for 
      // beginners that are cutting/pasting from the reference.
      String sizing = 
        "[\\s\\;]size\\s*\\(\\s*(\\S+)\\s*,\\s*(\\S+)\\s*\\);";
      Pattern pattern = compiler.compile(sizing);

      // adds a space at the beginning, in case size() is the very 
      // first thing in the program (very common), since the regexp 
      // needs to check for things in front of it.
      PatternMatcherInput input = new PatternMatcherInput(" " + program);
      if (matcher.contains(input, pattern)) {
        MatchResult result = matcher.getMatch();
        try {
          wide = Integer.parseInt(result.group(1).toString());
          high = Integer.parseInt(result.group(2).toString());

        } catch (NumberFormatException e) {
          // found a reference to size, but it didn't 
          // seem to contain numbers
          final String message = 
            "The size of this applet could not automatically be\n" +
            "determined from your code. You'll have to edit the\n" + 
            "HTML file to set the size of the applet.";

          PdeBase.showWarning("Could not find applet size", message, null);
        }
      }  // else no size() command found

      // handle this in editor instead, rare or nonexistant
      //} catch (MalformedPatternException e) {
      //PdeBase.showWarning("Internal Problem",
      //                    "An internal error occurred while trying\n" + 
      //                    "to export the sketch. Please report this.", e);
      //return false;
      //}

      StringBuffer sources = new StringBuffer();
      for (int i = 0; i < codeCount; i++) {
        sources.append("<a href=\'" + code[i].file.getName() + "\">" + 
                       code[i].name + "</a> ");
      }

      File htmlOutputFile = new File(appletDir, "index.html");
      FileOutputStream fos = new FileOutputStream(htmlOutputFile);
      PrintStream ps = new PrintStream(fos);

      // @@sketch@@, @@width@@, @@height@@, @@archive@@, @@source@@

      InputStream is = PdeBase.getStream("applet.html");
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));

      String line = null;
      while ((line = reader.readLine()) != null) {
        if (line.indexOf("@@") != -1) {
          StringBuffer sb = new StringBuffer(line);
          int index = 0;
          while ((index = sb.indexOf("@@sketch@@")) != -1) {
            sb.replace(index, index + "@@sketch@@".length(), 
                       name);
          }
          while ((index = sb.indexOf("@@source@@")) != -1) {
            sb.replace(index, index + "@@source@@".length(), 
                       sources.toString());
          }
          while ((index = sb.indexOf("@@archive@@")) != -1) {
            sb.replace(index, index + "@@archive@@".length(), 
                       name + ".jar");
          }
          while ((index = sb.indexOf("@@width@@")) != -1) {
            sb.replace(index, index + "@@width@@".length(), 
                       String.valueOf(wide));
          }
          while ((index = sb.indexOf("@@height@@")) != -1) {
            sb.replace(index, index + "@@height@@".length(), 
                       String.valueOf(wide));
          }
          line = sb.toString();
        }
        ps.println(line);
      }

      reader.close();
      ps.flush();
      ps.close();
    }

    // copy the source files to the target, since we like
    // to encourage people to share their code
    for (int i = 0; i < codeCount; i++) {
      PdeBase.copyFile(code[i].file, 
                       new File(appletDir, code[i].file.getName()));
    }

    // create new .jar file
    FileOutputStream zipOutputFile = 
      new FileOutputStream(new File(appletDir, name + ".jar"));
    ZipOutputStream zos = new ZipOutputStream(zipOutputFile);
    ZipEntry entry;

    // add the contents of the code folder to the jar
    // unpacks all jar files 
    File codeFolder = new File(sketchDir, "code");
    if (codeFolder.exists()) {
      String includes = PdeCompiler.contentsToClassPath(codeFolder);
      packClassPathIntoZipFile(includes, zos);
    }

    // add the appropriate bagel to the classpath
    String jdkVersionStr = PdePreferences.get("compiler.jdk_version");
    String bagelJar = "lib/export11.jar";  // default
    if (jdkVersion.equals("1.3") || jdkVersion.equals("1.4")) {
      bagelJar = "lib/export13.jar";
    }
    //if (jdkVersionStr.equals("1.3")) { bagelJar = "export13.jar" };
    //if (jdkVersionStr.equals("1.4")) { bagelJar = "export14.jar" };
    packClassPathIntoZipFile(bagelJar);

    /*
      // add the contents of lib/export to the jar file
      // these are the jdk11-only bagel classes
      String exportDir = ("lib" + File.separator + 
                          "export" + File.separator);
      String bagelClasses[] = new File(exportDir).list();

      for (int i = 0; i < bagelClasses.length; i++) {
        if (!bagelClasses[i].endsWith(".class")) continue;
        entry = new ZipEntry(bagelClasses[i]);
        zos.putNextEntry(entry);
        zos.write(PdeBase.grabFile(new File(exportDir + bagelClasses[i])));
        zos.closeEntry();
      }
    */

    // TODO these two loops are insufficient.
    // should instead recursively add entire contents of build folder
    // the data folder will already be a subdirectory
    // and the classes may be buried in subfolders if a package name was used

    // files to include from data directory
    if ((dataDir != null) && (dataDir.exists())) {
      String datafiles[] = dataDir.list();
      for (int i = 0; i < datafiles.length; i++) {
        // don't export hidden files
        // skipping dot prefix removes all: . .. .DS_Store
        if (datafiles[i].charAt(0) == '.') continue;

        entry = new ZipEntry(datafiles[i]);
        zos.putNextEntry(entry);
        zos.write(PdeBase.grabFile(new File(dataDir, datafiles[i])));
        zos.closeEntry();
      }
    }

    // add the project's .class files to the jar
    // just grabs everything from the build directory
    // since there may be some inner classes
    // (add any .class files from the applet dir, then delete them)
    String classfiles[] = appletDir.list();
    for (int i = 0; i < classfiles.length; i++) {
      if (classfiles[i].endsWith(".class")) {
        entry = new ZipEntry(classfiles[i]);
        zos.putNextEntry(entry);
        zos.write(PdeBase.grabFile(new File(appletDir, classfiles[i])));
        zos.closeEntry();
      }
    }

    // remove the .class files from the applet folder. if they're not 
    // removed, the msjvm will complain about an illegal access error, 
    // since the classes are outside the jar file.
    for (int i = 0; i < classfiles.length; i++) {
      if (classfiles[i].endsWith(".class")) {
        File deadguy = new File(appletDir, classfiles[i]);
        if (!deadguy.delete()) {
          PdeBase.showWarning("Could not delete", 
                              classfiles[i] + " could not \n" +
                              "be deleted from the applet folder.  \n" + 
                              "You'll need to remove it by hand.", null);
        }
      }
    }

    // close up the jar file
    zos.flush();
    zos.close();

    PdeBase.openFolder(appletDir);

    //} catch (Exception e) {
    //e.printStackTrace();
    //}
    return true;
  }


  /**
   * Slurps up .class files from a colon (or semicolon on windows) 
   * separated list of paths and adds them to a ZipOutputStream.
   */
  static public void packClassPathIntoZipFile(String path, 
                                              ZipOutputStream zos) 
    throws IOException {
    String pieces[] = 
      BApplet.splitStrings(path, File.pathSeparatorChar);

    for (int i = 0; i < pieces.length; i++) {
      if (pieces[i].length() == 0) continue;
      //System.out.println("checking piece " + pieces[i]);

      // is it a jar file or directory?
      if (pieces[i].toLowerCase().endsWith(".jar") || 
          pieces[i].toLowerCase().endsWith(".zip")) {
        try {
          ZipFile file = new ZipFile(pieces[i]);
          Enumeration entries = file.entries();
          while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            if (entry.isDirectory()) {
              // actually 'continue's for all dir entries

            } else {
              String name = entry.getName();
              // ignore contents of the META-INF folders
              if (name.indexOf("META-INF") == 0) continue;
              ZipEntry entree = new ZipEntry(name);

              zos.putNextEntry(entree);
              byte buffer[] = new byte[(int) entry.getSize()];
              InputStream is = file.getInputStream(entry);

              int offset = 0;
              int remaining = buffer.length; 
              while (remaining > 0) {
                int count = is.read(buffer, offset, remaining);
                offset += count;
                remaining -= count;
              }

              zos.write(buffer);
              zos.flush();
              zos.closeEntry();
            }
          }
        } catch (IOException e) {
          System.err.println("Error in file " + pieces[i]);
          e.printStackTrace();
        }
      } else {  // not a .jar or .zip, prolly a directory
        File dir = new File(pieces[i]);
        // but must be a dir, since it's one of several paths
        // just need to check if it exists
        if (dir.exists()) {
          packClassPathIntoZipFileRecursive(dir, null, zos);
        }
      }
    }
  }


  /**
   * Continue the process of magical exporting. This function
   * can be called recursively to walk through folders looking
   * for more goodies that will be added to the ZipOutputStream.
   */
  static public void packClassPathIntoZipFileRecursive(File dir, 
                                                       String sofar, 
                                                       ZipOutputStream zos) 
    throws IOException {
    String files[] = dir.list();
    for (int i = 0; i < files.length; i++) {
      //if (files[i].equals(".") || files[i].equals("..")) continue;
      // ignore . .. and .DS_Store
      if (files[i].charAt(0) == '.') continue;

      File sub = new File(dir, files[i]);
      String nowfar = (sofar == null) ? 
        files[i] : (sofar + "/" + files[i]);

      if (sub.isDirectory()) {
        packClassPathIntoZipFileRecursive(sub, nowfar, zos);

      } else {
        // don't add .jar and .zip files, since they only work
        // inside the root, and they're unpacked
        if (!files[i].toLowerCase().endsWith(".jar") &&
            !files[i].toLowerCase().endsWith(".zip") &&
            files[i].charAt(0) != '.') {
          ZipEntry entry = new ZipEntry(nowfar);
          zos.putNextEntry(entry);
          zos.write(PdeBase.grabFile(sub));
          zos.closeEntry();
        }
      }
    }
  }


  /**
   * Returns true if this is a read-only sketch. Used for the 
   * examples directory, or when sketches are loaded from read-only
   * volumes or folders without appropraite permissions.
   */
  public boolean isReadOnly() {
    return false;
  }


  // move things around in the array (as opposed to full reload)

  // may need to call setCurrent() if the tab was the last one
  // or maybe just call setCurrent(0) for good measure

  // don't allow the user to hide the 0 tab (the main file)

  public void hide(int which) {
  }


  /**
   * Path to the data folder of this sketch.
   */
  /*
  public File getDataDirectory() {
    File dataDir = new File(directory, "data");
    return dataDir.exists() ? dataDir : null;
  }
  */


  /**
   * Returns path to the main .pde file for this sketch.
   */
  public String getMainFilePath() {
    return files[0].getAbsolutePath();
  }
}


class PdeCode {
  String name;  // pretty name (no extension), not the full file name
  File file;
  int flavor;

  String program;
  boolean modified;
  //History history;  // later


  public PdeCode(String name, File file, int flavor) {
    this.name = name;
    this.file = file;
    this.flavor = flavor;
  }


  public void load() {
    program = null;
    try {
      if (files[i].length() != 0) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(files[i])));
        StringBuffer buffer = new StringBuffer();
        String line = null;
        while ((line = reader.readLine()) != null) {
          buffer.append(line);
          buffer.append('\n');
        }
        reader.close();
        program = buffer.toString();

      } else {
        // empty code file.. no worries, might be getting filled up
        program = "";
      }

    } catch (IOException e) {
      PdeBase.showWarning("Error loading file", 
                          "Error while opening the file\n" + 
                          files[i].getPath(), e);
      program = null;  // just in case
    }

    //if (program != null) {
    //history = new History(file);
    //}
  }
}
