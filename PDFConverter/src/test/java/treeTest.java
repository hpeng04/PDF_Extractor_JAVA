import org.apache.poi.ss.formula.functions.T;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import util.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Disabled
public class treeTest {

    @Test
    public void test1() {

        int[] list = {1, 2, 3, 4, 6, 7, 8};
        int[] temp = new int[list.length - 4];
        System.arraycopy(list, 4, temp, 0, list.length - 4);
        list[4] = 5;
        System.arraycopy(temp, 0, list, 5, temp.length - 1);
        System.out.println(Arrays.toString(list));
    }
}
