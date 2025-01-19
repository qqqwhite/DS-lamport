public class ts {
    public static void main(String[] args) {
        String str = "result";
        String result = "";
        for (int i = 0; i < str.length(); i++)
        {
            int index = (i + 3) % str.length();
            result = str.substring(index, index + 1) + result;
        }
        System.out.println(result);
    }
}
