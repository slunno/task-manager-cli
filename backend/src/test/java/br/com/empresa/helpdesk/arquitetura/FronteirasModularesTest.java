package br.com.empresa.helpdesk.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
    packages = "br.com.empresa.helpdesk",
    importOptions = ImportOption.DoNotIncludeTests.class)
class FronteirasModularesTest {
  @ArchTest
  static final ArchRule controllersNaoAcessamRepositorios =
      noClasses()
          .that()
          .resideInAPackage("..api..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..infra..");

  @ArchTest
  static final ArchRule repositoriosDeCategoriasNaoVazam =
      noClasses()
          .that()
          .resideOutsideOfPackage("..admin..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..admin.infra..");

  @ArchTest
  static final ArchRule repositoriosDeUsuariosNaoVazam =
      noClasses()
          .that()
          .resideOutsideOfPackage("..usuarios..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..usuarios.infra..");

  @ArchTest
  static final ArchRule repositoriosDeChamadosNaoVazam =
      noClasses()
          .that()
          .resideOutsideOfPackage("..chamados..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..chamados.infra..");

  @ArchTest
  static final ArchRule dominioNaoDependeDeCamadasExternas =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..api..", "..application..", "..infra..");
}
