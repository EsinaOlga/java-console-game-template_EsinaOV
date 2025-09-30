package com.example.dungeon.core;

import com.example.dungeon.model.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Game {
    private final GameState state = new GameState();
    private final Map<String, Command> commands = new LinkedHashMap<>();
    // Ошибка компиляции: пропущена точка с запятой после объявления поля
    // private int score  // Ошибка: ; expected

    static {
        WorldInfo.touch("Game");
    }

    public Game() {
        registerCommands();
        bootstrapWorld();
    }

    private void registerCommands() {
        commands.put("help", (ctx, a) -> System.out.println("Команды: " + String.join(", ", commands.keySet())));
        commands.put("gc-stats", (ctx, a) -> {
            Runtime rt = Runtime.getRuntime();
            long free = rt.freeMemory(), total = rt.totalMemory(), used = total - free;
            System.out.println("Память: used=" + used + " free=" + free + " total=" + total);
        });
        // Ошибка компиляции: отсутствует закрывающая скобка в лямбде
        /*
        commands.put("help", (ctx, a) -> {
            System.out.println("Команды: " + String.join(", ", commands.keySet()));
        // Ошибка: '}' expected
        */
        // Ошибка компиляции: неправильный тип переменной или несоответствие типов
        /*
        commands.put("gc-stats", (ctx, a) -> {
            String free = "text";
            long total = rt.totalMemory();
            long used = total - free; // Ошибка: нельзя вычесть String из long
            System.out.println("Память: used=" + used + " free=" + free + " total=" + total);
        });
        */
        commands.put("about", (ctx, a) -> {
            System.out.println("DungeonMini — простая текстовая игра-dungeon.");
            System.out.println("Версия 1.0");
            System.out.println("Автор: innopolisi whith Esina OV");
            System.out.println("Используйте команды для управления: help для списка.");
        });
        commands.put("look", (ctx, a) -> System.out.println(ctx.getCurrent().describe()));
        //  commands.put("move", (ctx, a) -> {
        //    throw new InvalidCommandException("TODO-1: реализуйте перемещение игрока");
        //});

        commands.put("move", (ctx, a) -> {
            if (a.isEmpty()) {
                throw new InvalidCommandException("Куда идти? Укажите направление: north, south, east, west.");
            }
            String direction = a.get(0).toLowerCase(Locale.ROOT);
            Set<String> validDirections = Set.of("north", "south", "east", "west");
            if (!validDirections.contains(direction)) {
                throw new InvalidCommandException("Неверное направление. Используйте: north, south, east, west.");
            }
            Room next = ctx.getCurrent().getNeighbors().get(direction);
            if (next == null) {
                throw new InvalidCommandException("Там нет пути: " + direction);
            }
            ctx.setCurrent(next);
            System.out.println("Вы переместились в " + next.getName() + ".");
            System.out.println(next.describe());
        });

        // Ошибка компиляции: вызов несуществующего конструктора
        // Player hero = new Player("Герой", 20); // Ошибка: конструктор Player(String, int) не найден
        //}

        //  commands.put("take", (ctx, a) -> {
        //    throw new InvalidCommandException("TODO-2: реализуйте взятие предмета");
        //});
        commands.put("take", (ctx, a) -> {
            if (a.isEmpty()) {
                throw new InvalidCommandException("Что брать? Укажите название предмета.");
            }
            String itemName = String.join(" ", a); // поддержка имен из нескольких слов
            Optional<Item> maybeItem = ctx.getCurrent().getItems().stream()
                    .filter(i -> i.getName().equalsIgnoreCase(itemName))
                    .findFirst();
            if (maybeItem.isEmpty()) {
                throw new InvalidCommandException("Такого предмета здесь нет: " + itemName);
            }
            Item item = maybeItem.get();
            ctx.getCurrent().getItems().remove(item);
            ctx.getPlayer().getInventory().add(item);
            System.out.println("Вы взяли: " + item.getName());
        });



        //commands.put("inventory", (ctx, a) -> {
         //   System.out.println("TODO-3: вывести инвентарь (Streams)");
        //});
        commands.put("inventory", (ctx, a) -> {
            List<Item> inv = ctx.getPlayer().getInventory();
            if (inv.isEmpty()) {
                System.out.println("Инвентарь пуст.");
            } else {
                String itemsList = inv.stream()
                        .map(Item::getName)
                        .reduce((s1, s2) -> s1 + ", " + s2)
                        .orElse("");
                System.out.println("Инвентарь: " + itemsList);
            }
        });

        //commands.put("use", (ctx, a) -> {
         //   throw new InvalidCommandException("TODO-4: реализуйте использование предмета");
        //});
        commands.put("use", (ctx, a) -> {
            if (a.isEmpty()) {
                throw new InvalidCommandException("Что использовать? Укажите название предмета.");
            }
            String itemName = String.join(" ", a);
            Optional<Item> maybeItem = ctx.getPlayer().getInventory().stream()
                    .filter(i -> i.getName().equalsIgnoreCase(itemName))
                    .findFirst();
            if (maybeItem.isEmpty()) {
                throw new InvalidCommandException("В инвентаре нет такого предмета: " + itemName);
            }
            Item item = maybeItem.get();
            if (item instanceof Potion) {
                Potion potion = (Potion) item;
                ctx.getPlayer().setHp(ctx.getPlayer().getHp() + potion.getHeal());
                ctx.getPlayer().getInventory().remove(item);
                System.out.println("Вы использовали " + potion.getName() + ". HP восстановлено на " + potion.getHeal()+" Текущее HP "+ ctx.getPlayer().getHp());
            } else {
                throw new InvalidCommandException("Этот предмет нельзя использовать прямо сейчас.");
            }
        });

        //commands.put("fight", (ctx, a) -> {
         //   throw new InvalidCommandException("TODO-5: реализуйте бой");
       // });

        commands.put("fight", (ctx, a) -> {
            Monster monster = ctx.getCurrent().getMonster();
            if (monster == null) {
                System.out.println("Здесь нет врагов для боя.");
                return;
            }
            Player player = ctx.getPlayer();
            // Пример ошибки ArithmeticException: деление на ноль
            /*
            int damage = player.getAttack();
            int divider = 0;
            int riskyValue = damage / divider; // ArithmeticException: деление на ноль
            */

            // Игрок наносит урон монстру
            monster.setHp(monster.getHp() - player.getAttack());
            System.out.println("Вы бьете " + monster.getName() + " на " + player.getAttack() + " урона. HP монстра "+monster.getHp());

            if (monster.getHp() <= 0) {
                System.out.println("Вы победили " + monster.getName() + "!");

                // Выпадение лута
                //  int loot = monster.getLevel();
                // if (false) {
                //    boolean add;
                //     if (!player.getInventory().add(loot)) add = false;
                //    else {
                //        add = true;
                //   }
                //  System.out.println("Вы получили лут: " + loot);
                // }

                // Удаляем монстра из комнаты
                ctx.getCurrent().setMonster(null);

                // Добавляем очки игроку
                ctx.addScore(10);
                return;
            }

            // Монстр наносит урон игроку
            player.setHp(player.getHp() - player.getAttack());
            System.out.println(monster.getName() + " отвечает на " +player.getAttack() + " урона. Ваше HP " +  player.getHp() );

            if (player.getHp() <= 0) {
                System.out.println("Вы проиграли бой и погибли... Игра окончена.");
                System.exit(0);
            } else {
                System.out.println("Ваше HP: " + player.getHp() + ", HP монстра: " + monster.getHp());
            }
        });

        commands.put("save", (ctx, a) -> SaveLoad.save(ctx));
        commands.put("load", (ctx, a) -> SaveLoad.load(ctx));
        commands.put("scores", (ctx, a) -> SaveLoad.printScores());
        commands.put("exit", (ctx, a) -> {
            System.out.println("Пока!");
            System.exit(0);
        });
    }

    private void bootstrapWorld() {
        Player hero = new Player("Герой", 20, 5);
        state.setPlayer(hero);

        Room square = new Room("Площадь", "Каменная площадь с фонтаном.");
        Room forest = new Room("Лес", "Шелест листвы и птичий щебет.");
        Room cave = new Room("Пещера", "Темно и сыро.");
        square.getNeighbors().put("north", forest);
        forest.getNeighbors().put("south", square);
        forest.getNeighbors().put("east", cave);
        cave.getNeighbors().put("west", forest);

        forest.getItems().add(new Potion("Малое зелье", 5));
        forest.setMonster(new Monster("Волк", 1, 8));

        state.setCurrent(square);
    }

    public void run() {
        System.out.println("DungeonMini (TEMPLATE). 'help' — команды.");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String line = in.readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;
                List<String> parts = Arrays.asList(line.split("\s+"));
                String cmd = parts.getFirst().toLowerCase(Locale.ROOT);
                List<String> args = parts.subList(1, parts.size());
                Command c = commands.get(cmd);
                try {
                    if (c == null) throw new InvalidCommandException("Неизвестная команда: " + cmd);
                    c.execute(state, args);
                    state.addScore(1);
                } catch (InvalidCommandException e) {
                    System.out.println("Ошибка: " + e.getMessage());
                } catch (Exception e) {
                    System.out.println("Непредвиденная ошибка: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Ошибка ввода/вывода: " + e.getMessage());
        }
    }
}
