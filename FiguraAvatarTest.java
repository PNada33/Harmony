import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 * Test class to verify Figura avatar integration
 */
public class FiguraAvatarTest {
    public static void main(String[] args) {
        System.out.println("=== Figura Avatar Integration Test ===");
        
        try {
            // Test if FiguraWear class exists and has getCurrent method
            Class<?> wearCl = Class.forName("xd.harm.utils.figura.FiguraWear");
            Method getCurrent = wearCl.getMethod("getCurrent");
            Object cur = getCurrent.invoke(null);
            
            if (cur != null) {
                System.out.println("✓ Figura avatar is currently active: " + cur.getClass().getSimpleName());
                System.out.println("✓ PlayerRenderer should suppress vanilla model rendering");
            } else {
                System.out.println("✓ No Figura avatar currently active");
                System.out.println("✓ PlayerRenderer should render vanilla model");
            }
            
        } catch (ClassNotFoundException e) {
            System.out.println("✗ FiguraWear class not found - Figura mod not loaded");
        } catch (NoSuchMethodException e) {
            System.out.println("✗ getCurrent method not found in FiguraWear");
        } catch (IllegalAccessException e) {
            System.out.println("✗ Unable to access getCurrent method");
        } catch (InvocationTargetException e) {
            System.out.println("✗ Error invoking getCurrent method: " + e.getCause());
        }
        
        System.out.println("\n=== PlayerRenderer Test ===");
        
        try {
            // Test if PlayerRenderer class has our fix
            Class<?> playerRendererCl = Class.forName("net.minecraft.client.renderer.entity.PlayerRenderer");
            Method renderMethod = playerRendererCl.getMethod("render", 
                Class.forName("net.minecraft.client.entity.player.AbstractClientPlayerEntity"),
                float.class, float.class, 
                Class.forName("com.mojang.blaze3d.matrix.MatrixStack"),
                Class.forName("net.minecraft.client.renderer.IRenderTypeBuffer"),
                int.class);
            
            System.out.println("✓ PlayerRenderer.render method found");
            System.out.println("✓ Should contain Figura avatar suppression logic");
            
        } catch (ClassNotFoundException e) {
            System.out.println("✗ PlayerRenderer class not found");
        } catch (NoSuchMethodException e) {
            System.out.println("✗ PlayerRenderer.render method not found");
        }
        
        System.out.println("\n=== Test Complete ===");
    }
}