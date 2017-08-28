package rsalesc.brazil;

/**
 * Created by Roberto Sales on 25/08/17.
 */
public class CharArray {
    char[] array;
    int length = 2;

    public CharArray() {
        array = new char[length];
    }

    public CharArray(String string) {
        length = Math.max(string.length(), 2);
        char[] array = new char[length];
        for(int i = 0; i < string.length(); i++) {
            array[i] = string.charAt(i);
        }
    }

    public void append(char c) {
        if(length >= array.length) {
            char[] newArray = new char[length * 2];
            System.arraycopy(array, 0, newArray, 0, length);
            array = newArray;
        }

        array[length++] = c;
    }

    public void set(int i, char c) {
        if(i < 0 || i >= length)
            throw new ArrayIndexOutOfBoundsException();
        array[i] = c;
    }

    public char get(int i) {
        if(i < 0 || i >= length)
            throw new ArrayIndexOutOfBoundsException();
        return array[i];
    }
}
