public class MainTest {
    public static class A {
        public void sayHello() {
            System.out.println("sayHello呀！");
        }
    }

    public static void main(String[] args) {
        System.out.println(add(122, 345));

        new A().sayHello();
    }

    public static int add(int a, int b) {
        return a + b;
    }
}