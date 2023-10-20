/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders;

import java.io.File;
import java.io.IOException;

import javax.enterprise.inject.Produces;
import javax.servlet.ServletException;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.util.FileUtils;
import org.codedefenders.util.WeldExtension;
import org.codedefenders.util.WeldSetup;
import org.jboss.weld.junit4.WeldInitiator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Jose Rojas
 */
@ExtendWith(WeldExtension.class)
public class GameManagerTest {

    // Required for mocking Configuration, which is loaded into a static field of FileUtils.
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.of(GameManagerTest.class);

    @Produces
    public Configuration produceConfiguration() {
        return new Configuration() {};
    }


    @Test
    public void testGetNextSubDirEmpty() throws IOException {
        File folder = getCleanTmpGameDir(1);
        assertEquals(folder.getAbsolutePath() + File.separator + "00000001", FileUtils.getNextSubDir(folder.toPath()).getAbsolutePath());
    }

    @Test
    public void testGetNextSubDirNonEmpty() throws IOException {
        File folder = getCleanTmpGameDir(1);
        File subfolder = new File(folder.getAbsolutePath() + File.separator + "00000001");
        subfolder.delete();
        subfolder.mkdir();
        assertEquals(folder.getAbsolutePath() + File.separator + "00000002", FileUtils.getNextSubDir(folder.toPath()).getAbsolutePath());
    }

    @Test
    public void testGetNextSubDirTwo() throws IOException {
        File folder = getCleanTmpGameDir(1);
        File subfolder = new File(folder.getAbsolutePath() + File.separator + "00000001");
        subfolder.delete();
        subfolder.mkdir();
        File subfolder2 = new File(folder.getAbsolutePath() + File.separator + "00000002");
        subfolder2.delete();
        subfolder2.mkdir();
        assertEquals(folder.getAbsolutePath() + File.separator + "00000003", FileUtils.getNextSubDir(folder.toPath()).getAbsolutePath());
    }

    @Test
    public void testGetNextSubDirMore() throws IOException, ServletException {
        File folder = getCleanTmpGameDir(1);
        File subfolder = new File(folder.getAbsolutePath() + File.separator + "00000001");
        subfolder.delete();
        subfolder.mkdir();
        File subfolder2 = new File(folder.getAbsolutePath() + File.separator + "00000002");
        subfolder2.delete();
        subfolder2.mkdir();
        File subfolder3 = new File(folder.getAbsolutePath() + File.separator + "foo");
        subfolder3.delete();
        subfolder3.mkdir();
        assertEquals(folder.getAbsolutePath() + File.separator + "00000003", FileUtils.getNextSubDir(folder.toPath()).getAbsolutePath());
    }

    private File getCleanTmpGameDir(int gameId) throws IOException {
        File folder = new File(org.apache.commons.io.FileUtils.getTempDirectory().getAbsolutePath() + File.separator + "testCodeDefenders" + File.separator + gameId);
        folder.delete();
        folder.mkdirs();
        org.apache.commons.io.FileUtils.cleanDirectory(folder);
        return folder;
    }
}
