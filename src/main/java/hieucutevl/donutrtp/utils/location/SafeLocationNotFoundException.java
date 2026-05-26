package hieucutevl.donutrtp.utils.location;

public class SafeLocationNotFoundException extends Exception {
   public SafeLocationNotFoundException(int tries) {
      super("Failed to find a safe location after " + tries + " attempts.");
   }
}
