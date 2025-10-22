# VM Translator - Nand2Tetris (Capítulos 7 y 8)

Traductor de código de Máquina Virtual (VM) a lenguaje ensamblador Hack para el proyecto Nand2Tetris.

## Descripción

Este proyecto implementa un traductor completo que convierte programas escritos en el lenguaje VM de Nand2Tetris a código ensamblador Hack. Soporta comandos aritméticos, acceso a memoria, flujo de control y llamadas a funciones.

## Características

### Comandos Soportados

#### Operaciones Aritméticas y Lógicas
- `add`, `sub`, `neg`
- `eq`, `gt`, `lt`
- `and`, `or`, `not`

#### Acceso a Memoria (Push/Pop)
- `constant` - Valores constantes
- `local` - Variables locales (LCL)
- `argument` - Argumentos de función (ARG)
- `this`, `that` - Punteros (THIS, THAT)
- `temp` - Memoria temporal (RAM 5-12)
- `pointer` - Acceso directo a THIS/THAT
- `static` - Variables globales

#### Flujo de Control
- `label etiqueta` - Define una etiqueta
- `goto etiqueta` - Salto incondicional
- `if-goto etiqueta` - Salto condicional

#### Funciones
- `function nombre nVars` - Declaración de función
- `call nombre nArgs` - Llamada a función
- `return` - Retorno de función

## Compilación

Para compilar el proyecto, ejecuta:

```bash
javac *.java
```

Esto generará los archivos `.class` necesarios para ejecutar el traductor.

## Uso

### Traducir un archivo individual

```bash
java VMTranslator <archivo.vm>
```

**Ejemplo:**
```bash
java VMTranslator example.vm
```

Esto generará `example.asm` en el mismo directorio.

### Traducir una carpeta

```bash
java VMTranslator <carpeta>
```

**Ejemplo:**
```bash
java VMTranslator MiPrograma/
```

Esto generará `MiPrograma/MiPrograma.asm` con el código de todos los archivos `.vm` de la carpeta.
