/*
 * The MIT License
 * 
 * Copyright (c) 2009, Ushus Technologies LTD.,Shinod K Mohandas
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.resultdependentshellscript;

import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.model.Result;
import hudson.util.EditDistance;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.DataBoundConstructor;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import hudson.tasks.Publisher;
import net.sf.json.JSONObject;

/**
 * @author Luke Melia
 */
public class ResultDependentShellScript extends Publisher {

  private String failureScript;
  private String fixedScript;

  @DataBoundConstructor
	public ResultDependentShellScript(String failureScript, String fixedScript) {
	  this.failureScript = failureScript;
	  this.fixedScript = fixedScript;
	}

  /**
   * Shell script to be executed when build fails.
   */
	public String getFailureScript() {
	  return failureScript;
	}

  /**
   * Shell script to be executed when build is fixed.
   */
	public String getFixedScript() {
	  return fixedScript;
	}

  @Override
  public boolean perform(AbstractBuild build, Launcher launcher,
      BuildListener listener) throws InterruptedException, IOException {
        
		listener.getLogger().println("Performing Result Dependent Shell Script task...");

  	if (isFailure(build)) {
			listener.getLogger().println("Running script  : " + failureScript);
			CommandInterpreter runner = getCommandInterpreter(launcher, failureScript);
			runner.perform(build, launcher, listener);
    } else if (isRecovery(build)) {
			listener.getLogger().println("Running script  : " + fixedScript);
			CommandInterpreter runner = getCommandInterpreter(launcher, fixedScript);
			runner.perform(build, launcher, listener);
    }
    return true;
  }
  
  protected boolean isFailure(AbstractBuild<?, ?> build) {
      return (build.getResult() == Result.FAILURE || build.getResult() == Result.UNSTABLE);
  }

  protected boolean isRecovery(AbstractBuild<?, ?> build) {
      if (build.getResult() == Result.SUCCESS) {
      	AbstractBuild<?, ?> previousBuild = build.getPreviousBuild();
        if (previousBuild != null && previousBuild.getResult() != Result.SUCCESS) {
            return true;
        } else {
            return false;
        }
      } else {
        return false;
      }
  }

	/**
	 * This method will return the command intercepter as per the node OS
	 * 
	 * @param launcher
	 * @param script
	 * @return CommandInterpreter
	 */
	private CommandInterpreter getCommandInterpreter(Launcher launcher, String script) {
		if (launcher.isUnix())
			return new Shell(script);
		else
			return new BatchFile(script);
	}

	/**
	 * This method will return the descriptorobject.
	 * 
	 * @return DESCRIPTOR
	 */
	public DescriptorImpl getDescriptor() {
		return DESCRIPTOR;
	}

	public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

	public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    private static final Logger LOGGER = Logger.getLogger(DescriptorImpl.class.getName());
	  public String failureScript;
    public String fixedScript;
    
		public DescriptorImpl() {
			super(ResultDependentShellScript.class);
			load();
		}

		@Override
    public boolean isApplicable(Class<? extends AbstractProject> jobType) {
      return true;
    }

		@Override
		public String getDisplayName() {
			return "Result-dependent Shell Script";
		}
		
		public String getFailureScript() {
		  return failureScript;
		}

		public String getFixedScript() {
		  return fixedScript;
		}

		@Override
		public String getHelpFile() {
			return "/plugin/result-dependent-shell-script/help/main.html";
		}

		@Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      LOGGER.info("configure in ResultDependentShellScript Descriptor");
      req.bindParameters(this, "result-dependent-shell-script.");
      save();
      return super.configure(req, formData);
    }
    
    @Override
		public Publisher newInstance(StaplerRequest req, JSONObject formData) throws FormException {
      String configuredFailureScript = req.getParameter("result-dependent-shell-script.failureScript");
      String configuredFixedScript = req.getParameter("result-dependent-shell-script.fixedScript");
      LOGGER.info("newInstance: " + configuredFailureScript);
      LOGGER.info("newInstance: " + configuredFixedScript);
			ResultDependentShellScript p = new ResultDependentShellScript(configuredFailureScript, configuredFixedScript);
			return p;
		}
		
	}
}
