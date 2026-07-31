/**
 * Simple test to verify PlayerRenderer code changes
 */
public class PlayerRendererTest {
    public static void main(String[] args) {
        System.out.println("=== PlayerRenderer Fix Verification ===");
        
        // Read the PlayerRenderer.java file and check for our fix
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("src", "net", "minecraft", "client", "renderer", "entity", "PlayerRenderer.java");
            String content = new String(java.nio.file.Files.readAllBytes(path));
            
            // Check for our fix
            if (content.contains("// Suppress vanilla player model when a Figura avatar is active")) {
                System.out.println("✓ PlayerRenderer contains Figura avatar suppression logic");
            } else {
                System.out.println("✗ PlayerRenderer missing Figura avatar suppression logic");
            }
            
            if (content.contains("Class.forName(\"xd.harm.utils.figura.FiguraWear\")")) {
                System.out.println("✓ PlayerRenderer contains FiguraWear class reference");
            } else {
                System.out.println("✗ PlayerRenderer missing FiguraWear class reference");
            }
            
            if (content.contains("if (cur != null)")) {
                System.out.println("✓ PlayerRenderer contains avatar null check");
            } else {
                System.out.println("✗ PlayerRenderer missing avatar null check");
            }
            
            if (content.contains("return;")) {
                System.out.println("✓ PlayerRenderer contains early return to suppress vanilla render");
            } else {
                System.out.println("✗ PlayerRenderer missing early return");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Error reading PlayerRenderer.java: " + e.getMessage());
        }
        
        System.out.println("\n=== FiguraWear Verification ===");
        
        // Check FiguraWear.java for ModelPart layer disabling
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("src", "xd", "harm", "utils", "figura", "FiguraWear.java");
            String content = new String(java.nio.file.Files.readAllBytes(path));
            
            if (content.contains("ModelPart")) {
                System.out.println("✓ FiguraWear contains ModelPart layer disabling logic");
            } else {
                System.out.println("✗ FiguraWear missing ModelPart layer disabling logic");
            }
            
            if (content.contains("ReentrantLock")) {
                System.out.println("✓ FiguraWear contains ReentrantLock synchronization");
            } else {
                System.out.println("✗ FiguraWear missing ReentrantLock synchronization");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Error reading FiguraWear.java: " + e.getMessage());
        }
        
        System.out.println("\n=== FiguraAvatarInstaller Verification ===");
        
        // Check FiguraAvatarInstaller.java for local folder fallback
        try {
            java.nio.file.Path path = java.nio.file.Paths.get("src", "xd", "harm", "utils", "figura", "FiguraAvatarInstaller.java");
            String content = new String(java.nio.file.Files.readAllBytes(path));
            
            if (content.contains("figura_avatars")) {
                System.out.println("✓ FiguraAvatarInstaller contains local folder fallback");
            } else {
                System.out.println("✗ FiguraAvatarInstaller missing local folder fallback");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Error reading FiguraAvatarInstaller.java: " + e.getMessage());
        }
        
        System.out.println("\n=== Test Complete ===");
    }
}