public class Helper {
    public static int clamp(int num, int min, int max) {
        return (num <= max && num >=min) ? num : 
               ((num <= max) ? min : max);
    }
}
