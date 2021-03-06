/*******************************************************************************
 * Copyright 2016 Jalian Systems Pvt. Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.sourceforge.marathon.javafxagent;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import net.sourceforge.marathon.javafxagent.IDevice.Buttons;

@Test public class EventQueueDeviceMouseTest extends EventQueueDeviceTest {

    public void testClick() {
        buttonClicked = false;
        EventQueueWait.requestFocus(button);
        driver.click(button, null, null, Buttons.LEFT, 1, 0, 0);
        new WaitWithoutException() {
            @Override public boolean until() {
                return buttonClicked;
            }
        }.wait("Button is not clicked", 3000, 500);
        AssertJUnit.assertEquals(true, buttonClicked);
    }

    public void testClick_withAlt() {
        mouseText.setLength(0);
        EventQueueWait.requestFocus(button);
        driver.pressKey(button, JavaAgentKeys.ALT);
        driver.click(button, null, null, Buttons.LEFT, 1, 0, 0);
        driver.releaseKey(button, JavaAgentKeys.ALT);
        final String expected = "Alt+Button1+entered Alt+Button1+pressed Alt+Button1+released Alt+Button1+clicked(1)";
        new WaitWithoutException() {
            @Override public boolean until() {
                return expected.equals(mouseText.toString().trim());
            }
        }.wait("Mouse text is empty", 3000, 500);
        AssertJUnit.assertEquals(expected, mouseText.toString().trim());
    }

    public void testClick_withAltControl() {
        mouseText.setLength(0);
        EventQueueWait.requestFocus(button);
        driver.pressKey(button, JavaAgentKeys.ALT);
        driver.pressKey(button, JavaAgentKeys.CONTROL);
        driver.click(button, null, null, Buttons.LEFT, 1, 0, 0);
        driver.releaseKey(button, JavaAgentKeys.CONTROL);
        driver.releaseKey(button, JavaAgentKeys.ALT);
        final String expected = "Ctrl+Alt+Button1+entered Ctrl+Alt+Button1+pressed Ctrl+Alt+Button1+released Ctrl+Alt+Button1+clicked(1)";
        new WaitWithoutException() {
            @Override public boolean until() {
                return expected.equals(mouseText.toString().trim());
            }
        }.wait("Mouse text is empty", 3000, 500);
        AssertJUnit.assertEquals(expected, mouseText.toString().trim());
    }

    public void testDoubleClick() {
        mouseText.setLength(0);
        buttonClicked = false;
        EventQueueWait.requestFocus(button);
        driver.click(button, null, null, Buttons.LEFT, 2, 0, 0);
        final String expected = "Button1+entered Button1+pressed Button1+released Button1+clicked(1) Button1+pressed Button1+released Button1+clicked(2)";
        new WaitWithoutException() {
            @Override public boolean until() {
                return buttonClicked && expected.equals(mouseText.toString().trim());
            }
        }.wait("Button is not clicked", 3000, 500);
        AssertJUnit.assertEquals(true, buttonClicked);
        AssertJUnit.assertEquals(expected, mouseText.toString().trim());
    }

    public void testMouseUp() {
        mouseText.setLength(0);
        EventQueueWait.requestFocus(button);
        driver.buttonDown(button, Buttons.LEFT, 0, 0);
        driver.buttonUp(button, Buttons.LEFT, 0, 0);
        final String expected = "Button1+pressed Button1+released";
        new WaitWithoutException() {
            @Override public boolean until() {
                return expected.equals(mouseText.toString().trim());
            }
        }.wait("Mouse text is empty", 3000, 500);
        AssertJUnit.assertEquals(expected, mouseText.toString().trim());
    }

    public void testContextClick() {
        mouseText.setLength(0);
        buttonClicked = false;
        EventQueueWait.requestFocus(button);
        driver.click(button, null, null, Buttons.RIGHT, 1, 0, 0);
        final String expected = "Button3+entered Button3+pressed Button3+released Button3+contextClicked(1)";
        new WaitWithoutException() {
            @Override public boolean until() {
                return expected.equals(mouseText.toString().trim());
            }
        }.wait("Mouse text is empty", 3000, 500);
        AssertJUnit.assertEquals(expected, mouseText.toString().trim());
    }

    public void testAltContextClick() {
        mouseText.setLength(0);
        buttonClicked = false;
        EventQueueWait.requestFocus(button);
        driver.pressKey(button, JavaAgentKeys.ALT);
        driver.click(button, null, null, Buttons.RIGHT, 1, 0, 0);
        driver.releaseKey(button, JavaAgentKeys.ALT);
        final String expected = "Alt+Button3+entered Alt+Button3+pressed Alt+Button3+released Alt+Button3+contextClicked(1)";
        new WaitWithoutException() {
            @Override public boolean until() {
                return MouseButton.SECONDARY == clickedButton && expected.equals(mouseText.toString().trim());
            }
        }.wait("Mouse text is empty", 3000, 500);
        AssertJUnit.assertEquals(expected, mouseText.toString().trim());
        AssertJUnit.assertEquals(MouseButton.SECONDARY, clickedButton);
    }

    public void testMouseMoveCoordinatesLongLong() {
        EventQueueWait.requestFocus(button);
        final StringBuilder text = new StringBuilder();
        button.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                if (e.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                    text.append("dragged ");
                }
                if (e.getEventType() == MouseEvent.MOUSE_MOVED) {
                    text.append("moved(" + e.getX() + "," + e.getY() + ") ");
                }
            }
        });
        driver.moveto(button, 3, 3);
        driver.moveto(button, 5, 10);
        final String expected = "moved(3.0,3.0) moved(5.0,10.0)";
        new WaitWithoutException() {
            @Override public boolean until() {
                return expected.equals(text.toString().trim());
            }
        }.wait("Text is empty", 3000, 500);
        AssertJUnit.assertEquals(expected, text.toString().trim());
    }

    public void testMouseMoveCoordinatesLongLong_WithButtonPress() {
        EventQueueWait.requestFocus(button);
        driver.buttonDown(button, Buttons.LEFT, 0, 0);
        final StringBuilder text = new StringBuilder();
        button.addEventHandler(MouseEvent.ANY, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                if (e.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                    text.append("dragged(" + e.getX() + "," + e.getY() + ") ");
                }
                if (e.getEventType() == MouseEvent.MOUSE_MOVED) {
                    text.append("moved(" + e.getX() + "," + e.getY() + ") ");
                }
            }
        });
        driver.moveto(button, 3, 3);
        driver.moveto(button, 5, 10);
        final String expected = "dragged(3.0,3.0) dragged(5.0,10.0)";
        new WaitWithoutException() {
            @Override public boolean until() {
                return expected.equals(text.toString().trim());
            }
        }.wait("Text is empty", 3000, 500);
        AssertJUnit.assertEquals(expected, text.toString().trim());
    }

    public void testButtonDownWithCoordinates() {
        EventQueueWait.requestFocus(button);
        final StringBuilder text = new StringBuilder();
        button.addEventHandler(MouseEvent.MOUSE_PRESSED, new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent e) {
                if (e.getEventType() == MouseEvent.MOUSE_PRESSED) {
                    text.append("pressed(" + e.getX() + "," + e.getY() + ") ");
                }
            }
        });
        String expected = "pressed(3.0,3.0)";
        try {
            driver.buttonDown(button, Buttons.LEFT, 3.0, 3.0);
            new WaitWithoutException() {
                @Override public boolean until() {
                    return expected.equals(text.toString().trim());
                }
            }.wait("Text is empty", 3000, 500);
        } finally {
            driver.buttonUp(button, Buttons.LEFT, 3.0, 3.0);
        }
        AssertJUnit.assertEquals(expected, text.toString().trim());
    }

}
