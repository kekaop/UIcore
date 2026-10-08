package org.orvencraft.uicore.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.api.ConfigurationCopy;

class ExtensionReferenceValidatorTest {
   @Test
   void reportsNestedReferencesWithPropertyPaths() throws Exception {
      YamlConfiguration yaml = new YamlConfiguration();
      yaml.loadFromString("""
         type: example:card
         name: 'Balance: {example:balance}'
         action:
           type: example:accept
         visible-if:
           all:
             - permission: example.open
             - not:
                 custom:
                   type: example:completed
         """);
      var issues = ExtensionReferenceValidator.find(ConfigurationCopy.model(yaml),
         (kind, id) -> Set.of("example:balance").contains(id));
      assertEquals(List.of(
         new ExtensionReferenceValidator.Issue("action.type", "action", "example:accept"),
         new ExtensionReferenceValidator.Issue("type", "component", "example:card"),
         new ExtensionReferenceValidator.Issue("visible-if.all[1].not.custom.type", "condition", "example:completed")
      ), issues);
   }
}
