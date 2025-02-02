package seng202.team6.util;

import java.io.IOException;
import java.io.InputStream;
import javafx.scene.image.Image;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class to manage reading an image.
 */
public class ImageReader {

  private static final Logger log = LogManager.getLogger(ImageReader.class);

  /**
   * Loads an image from disk.
   *
   * @param path image path
   * @return image
   */
  public static Image loadImage(String path) {
    try (InputStream inputStream = ImageReader.class.getResourceAsStream(path)) {
      if (inputStream != null) {
        return new Image(inputStream);
      }
      log.error("Could not load image at path {}}", path);
    } catch (IOException error) {
      log.error("Could load image", error);
    }
    return null;
  }

  /**
   * Loads an image asynchronously from the specified URL.
   *
   * @param url the URL of the image to be loaded
   * @return an Image object representing the loaded image
   */
  public static Image loadImageFromUrl(String url) {
    return new Image(url, true);
  }
}
