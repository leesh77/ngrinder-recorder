/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.ngrinder.recorder.infra;

import static net.grinder.util.NoOp.noOp;
import static net.grinder.util.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class which represents RecorderHome.
 * 
 * @author JunHo Yoon
 * @since 1.0
 */
public class RecorderHome {

	private final File directory;
	private static final Logger LOGGER = LoggerFactory.getLogger(RecorderHome.class);

	/**
	 * Constructor.
	 * 
	 * @param directory
	 *            agent home directory
	 */
	public RecorderHome(File directory) {
		checkNotNull(directory, "The directory should not be null.");
		if (StringUtils.contains(directory.getAbsolutePath().trim(), " ")) {
			throw processException(String.format(
							"nGrinder agent home directory \"%s\" should not contain space."
											+ "Please set NGRINDER_AGENT_HOME env var in the different location",
							directory.getAbsolutePath()));
		}

		if (!directory.exists() && !directory.mkdirs()) {
			throw processException(String.format(
							"nGrinder agent home directory %s is not created. Please check the permission",
							directory.getAbsolutePath()));
		}

		if (!directory.isDirectory()) {
			throw processException(String.format(
							"nGrinder home directory %s is not directory. Please delete this file in advance",
							directory.getAbsolutePath()));
		}

		if (!directory.canWrite()) {
			throw processException(String.format(
							"nGrinder home directory %s is not writable. Please adjust permission on this folder",
							directory));
		}

		this.directory = directory;
	}

	/**
	 * Get agent home directory.
	 * 
	 * @return agent home directory
	 */
	public File getDirectory() {
		return directory;
	}

	/**
	 * Copy {@link InputStream} to path in the target.
	 * 
	 * @param io
	 *            {@link InputStream}
	 * @param target
	 *            target path. only file name will be used.
	 * @param overwrite
	 *            true if overwrite
	 */
	public void copyFileTo(InputStream io, File target, boolean overwrite) {
		// Copy missing files
		try {
			target = new File(directory, target.getName());
			if (!(target.exists())) {
				FileUtils.writeByteArrayToFile(target, IOUtils.toByteArray(io));
			}
		} catch (IOException e) {
			String message = "Failed to write a file to " + target.getAbsolutePath();
			throw processException(message, e);
		}
	}

	/**
	 * Get properties from path.
	 * 
	 * @param path
	 *            property file path
	 * @return {@link Properties} instance. return empty property if it has problem.
	 */
	public Properties getProperties(String path) {
		Properties properties = new Properties();
		InputStream is = null;
		try {
			File propertiesFile = new File(directory, path);
			String config = FileUtils.readFileToString(propertiesFile, "UTF-8");
			properties.load(new StringReader(config));
		} catch (IOException e) {
			noOp();
		} finally {
			IOUtils.closeQuietly(is);
		}
		return properties;

	}

	/**
	 * Get file from path.
	 * 
	 * @param path
	 *            path
	 * @return {@link File} instance.
	 */
	public File getFile(String path) {
		return new File(getDirectory(), path);
	}

	/**
	 * Save properties.
	 * 
	 * @param path
	 *            path to save
	 * @param properties
	 *            properties.
	 */
	public void saveProperties(String path, Properties properties) {
		OutputStream out = null;
		try {
			File propertiesFile = new File(getDirectory(), path);
			out = FileUtils.openOutputStream(propertiesFile);
			properties.store(out, null);
		} catch (IOException e) {
			LOGGER.error("Could not save property  file on " + path, e);
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	public File getLogDirectory() {
		return new File(getDirectory(), "log");
	}
}
