package com.example.testeasyexcel.util;

import com.alibaba.fastjson.JSONArray;
import com.example.testeasyexcel.entity.Cell;
import com.example.testeasyexcel.entity.Table;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @BelongsProject:TestEasyExcel
 * @BelongsPackage:com.example.testeasyexcel.util
 * @Author:UestcXiye
 * @CreateTime:2024-04-21 22:23:23
 * @Description:
 */
public class DrawTableUtil {

    @SneakyThrows
    public static byte[] getStudentInfoImage(JSONArray array, List<Cell> headCells) {
        List<List<String>> tableRowContents = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            com.alibaba.fastjson.JSONObject json = array.getJSONObject(i);
            List<String> list = new ArrayList<>();
            for (String key : json.keySet()) {
                list.add(json.getString(key));
            }
            tableRowContents.add(list);
        }
        int[] mergeColumns = new int[]{1, 2};

        Table table = new Table().setCellFont(new Font("微软雅黑", Font.BOLD, 15))
                .setHeadCells(headCells).setHeaderFont(new Font("微软雅黑", Font.BOLD, 18))
                .setHeaderBackGroundColor(Color.yellow)
                .setMergeColumns(mergeColumns);
        BufferedImage image = drawTable(table, tableRowContents);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        String path = "C:\\Users\\szdx\\Desktop\\serviceCount.png";
//        ImageIO.write(image, "png", new File(path));
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    /**
     * 绘制表格
     *
     * @param table            表格属性
     * @param tableRowContents 表格内容
     */
    @SneakyThrows
    public static BufferedImage drawTable(Table table, List<List<String>> tableRowContents) {
        checkHead(table);
        List<Cell> headCells = table.getHeadCells();
        int[] mergeColumns = table.getMergeColumns();
        Map<Integer, List<Cell>> rows = headCells.stream().collect(Collectors.groupingBy(Cell::getRow));
        // 表头的最后一行实际有多少个单元格，有合并单元格的情况下按垂直投影的方式获取列；
        List<Cell> actualLastHeadRowColumnCell = findHeadColumns(headCells);
        HashMap<Integer, Integer> headRealRowHead = getHeadRowHeight(rows, table);
        HashMap<Integer, Integer> contentRowHeight = getContentRowHeight(tableRowContents, table);
        // 处理表头
        dealTableHead(rows, table, actualLastHeadRowColumnCell, headCells, headRealRowHead);
        // 生成表格各单元格内容对象
        List<Cell> contents = getTableContent(tableRowContents, actualLastHeadRowColumnCell, rows.size(), contentRowHeight);
        // 合并单元格
        List<Cell> cells = mergeCells(contents, mergeColumns, headCells);
        // 计算表格高度
        Integer tableHeight = Stream.of(headRealRowHead.values(), contentRowHeight.values())
                .flatMap(Collection::stream)
                .reduce(Integer::sum)
                .orElseThrow(() -> new RuntimeException("表格高度错误"));
        // 绘制表格
        return starDrawTable(rows.size(), cells, table, tableHeight);
    }

    /**
     * 表头格式校验
     *
     * @param table 表头参数信息
     */
    public static void checkHead(Table table) {
        if (Objects.isNull(table.getHeaderFont())) {
            table.setHeaderFont(new Font("楷体", Font.BOLD, 15));
        }
        if (Objects.isNull(table.getCellFont())) {
            table.setHeaderFont(new Font("宋体", Font.PLAIN, 12));
        }
        if (Objects.isNull(table.getHeaderBackGroundColor())) {
            table.setHeaderBackGroundColor(Color.gray);
        }
        if (table.getRowHeight() <= 30) {
            table.setRowHeight(30);
        }
        if (table.getMarginX() < 0 || table.getMarginY() < 0) {
            throw new RuntimeException("表格边距不可小于0");
        }
        table.getHeadCells().forEach(head -> {
            if (head.getRow() <= 0) {
                throw new RuntimeException("行不可小于0" + head.getRow() + "," + head.getColumn());
            }
            if (head.getColumn() <= 0) {
                throw new RuntimeException("列不可小于0" + head.getRow() + "," + head.getColumn());
            }
            if (head.getBelongColumn() < 0) {
                throw new RuntimeException("从属单元格列不可小于0" + head.getRow() + "," + head.getColumn());
            }
            if (head.getWidth() < 0) {
                throw new RuntimeException("列宽不可小于0" + head.getRow() + "," + head.getColumn());
            }
        });
        checkBelongs(table.getHeadCells());
        ImmutableTriple<ArrayList<Integer>, ArrayList<Integer>, ArrayList<Integer>> triple =
                table.getHeadCells().stream().reduce(ImmutableTriple.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>()),
                        (a, b) -> {
                            a.getLeft().add(b.getRow());
                            a.getMiddle().add(b.getColumn());
                            a.getRight().add(b.getBelongColumn());
                            return a;
                        }, (a, b) -> a);
        checkConsistency(triple.getLeft());
        checkConsistency(triple.getMiddle());
        table.getHeadCells().stream().map(Cell::getBelongColumn).distinct()
                .filter(column -> column > 0)
                .filter(column -> !triple.getMiddle().contains(column))
                .findAny().ifPresent(a -> {
                    throw new RuntimeException("从属单元格" + a + "列不存在");
                });
    }

    /**
     * 从属单元格校验
     *
     * @param headCells 表头
     */
    public static void checkBelongs(List<Cell> headCells) {
        Map<Integer, List<Cell>> collect = headCells.stream().collect(Collectors.groupingBy(Cell::getRow));
        collect.forEach((key, value) -> {
            List<Cell> rowCells = value.stream().sorted(Comparator.comparing(Cell::getColumn)).collect(Collectors.toList());
            int belongColumn = 0;
            for (Cell cell : rowCells) {
                if (cell.getBelongColumn() >= belongColumn) {
                    belongColumn = cell.getBelongColumn();
                    continue;
                }
                throw new RuntimeException("从属单元格配置错误" + cell.getRow() + "," + cell.getColumn());
            }
        });
    }

    /**
     * 表格连续性校验
     *
     * @param data 行或列序列
     */
    private static void checkConsistency(ArrayList<Integer> data) {
        List<Integer> collect = data.stream().distinct().sorted().collect(Collectors.toList());
        for (int i = 0; i < collect.size() - 1; i++) {
            Integer integer = collect.get(i);
            if (!collect.get(i + 1).equals(integer + 1)) {
                throw new RuntimeException("行或者列不连续");
            }
        }
    }

    /**
     * 获取表头最后一行的所有单元格
     *
     * @param cells 表头所有的单元格
     */
    public static List<Cell> findHeadColumns(List<Cell> cells) {
        Map<Integer, List<Cell>> rows = cells.stream().collect(Collectors.groupingBy(Cell::getRow));
        //取第一行的所有单元格
        List<Cell> firstRow = rows.get(1);
        if (CollectionUtils.isEmpty(firstRow)) {
            throw new RuntimeException("缺少第一行");
        }
        // 按列升序排序
        List<Cell> perRowCells = firstRow.stream().sorted(Comparator.comparing(Cell::getColumn)).collect(Collectors.toList());
        List<Cell> headColumns = new ArrayList<>();
        for (Cell cell : perRowCells) {
            // 获取每个单元格下的所有从属单元格，如果没有则返回其本身
            headColumns.addAll(getCellSonCells(cells, cell));
        }
        return headColumns;
    }

    /**
     * 获取所有的从属单元格
     *
     * @param cells 所有表头单元格
     * @param cell  要获取从属的单元格
     * @return 该单元格下的从属单元格
     */
    public static List<Cell> getCellSonCells(List<Cell> cells, Cell cell) {
        // 从属单元格：该单元格所在列中的所有单元格
        List<Cell> collect = cells.stream()
                .filter(cel -> cel.getRow() == cell.getRow() + 1)
                .filter(cel -> cel.getBelongColumn() == cell.getColumn())
                .collect(Collectors.toList());
        // 没有从属单元格则返回本身
        if (CollectionUtils.isEmpty(collect)) {
            ArrayList<Cell> objects = new ArrayList<>();
            objects.add(cell);
            return objects;
        }
        //有从属单元格则遍历每一个单元格来获取每个单元格下的从属单元格
        List<List<Cell>> allSonCells = collect.stream().map(s -> getCellSonCells(cells, s)).collect(Collectors.toList());
        return allSonCells.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    /**
     * 计算表头行高
     *
     * @param rows  表头行
     * @param table 表属性
     * @return 表头行与行高的对应 key:行 value:行高
     */
    private static HashMap<Integer, Integer> getHeadRowHeight(Map<Integer, List<Cell>> rows, Table table) {
        return rows.keySet().stream().map(row1 -> {
            Integer cellRow = rows.get(row1).stream().map(cell -> org.apache.commons.lang3.StringUtils.countMatches(cell.getContent(), "\n")).max(Integer::compare).orElse(1);
            int appendHeight = cellRow * table.getHeaderFont().getSize();
            return Pair.of(row1, table.getRowHeight() + appendHeight);
        }).reduce(new HashMap<>(16), (a, b) -> {
            a.put(b.getLeft(), b.getRight());
            return a;
        }, (c, d) -> c);
    }

    /**
     * 计算单元格高度
     *
     * @param tableRowContents 表格内容
     * @param table            表格属性
     * @return 行与行高对应关系  key:行 value:行高
     */
    private static HashMap<Integer, Integer> getContentRowHeight(List<List<String>> tableRowContents, Table table) {
        return IntStream.rangeClosed(1, tableRowContents.size())
                .mapToObj(row -> dealRowHeight(tableRowContents, table, row))
                .reduce(new HashMap<>(16), (a, b) -> {
                    a.put(b.getLeft(), b.getRight());
                    return a;
                }, (c, d) -> c);
    }

    /**
     * 计算单元格高度主方法
     *
     * @param tableRowContents 表格内容
     * @param table            表格属性
     * @param row              行内容
     * @return 行与行高的对应关系  key:行 value:行高
     */
    private static Pair<Integer, Integer> dealRowHeight(List<List<String>> tableRowContents, Table table, int row) {
        Integer cellRows = tableRowContents.get(row - 1).stream()
                .map(content -> org.apache.commons.lang3.StringUtils.countMatches(content, "\n")).max(Integer::compare).orElse(1);
        int appendHeight = table.getCellFont().getSize() * cellRows;
        return Pair.of(row, table.getRowHeight() + appendHeight);
    }

    /**
     * 处理表头
     *
     * @param rows                        表头行内容
     * @param table                       行高
     * @param actualLastHeadRowColumnCell 表头单元格
     * @param headCells                   表头单元格
     */
    public static void dealTableHead(Map<Integer, List<Cell>> rows, Table table, List<Cell> actualLastHeadRowColumnCell, List<Cell> headCells,
                                     HashMap<Integer, Integer> headRealRowHead) {
        int marginX = table.getMarginX();
        int marginY = table.getMarginY();
        rows.keySet().stream().sorted().forEach(row -> {
            List<Cell> perRowCells = rows.get(row).stream().sorted(Comparator.comparing(Cell::getColumn)).collect(Collectors.toList());
            int startFrom = marginX;
            // 非第一行的情况，有可能起始列不是第一列，这里需要取前几列的宽度作为起始宽度
            if (row > 1 && perRowCells.get(0).getColumn() > 1) {
                int column = perRowCells.get(0).getColumn();
                int offset = IntStream.range(1, column).map(index -> actualLastHeadRowColumnCell.get(index - 1).getWidth()).sum();
                startFrom = marginX + offset;
            }
            for (Cell cell : perRowCells) {
                // 获取从属单元格,单元格的宽度由从属单元格确定
                List<Cell> attached = getCellSonCells(headCells, cell);
                int cellWidth = attached.stream().mapToInt(Cell::getWidth).sum();
                cell.setHeight(headRealRowHead.get(row));
                // 如果该单元格有从属，则高度为行高，反之为单元格所跨行数*行高
                // attached
                //  长度为1：当不是自己时才说明该单元格是他的从属
                //  长度大于1：有从属
                if (attached.size() == 1 && attached.get(0).equals(cell)) {
                    int sum = IntStream.rangeClosed(cell.getRow(), rows.size()).map(headRealRowHead::get).sum();
                    cell.setHeight(sum);
                }
                if (cellWidth <= 0) {
                    throw new RuntimeException("行:" + cell.getRow() + " 列:" + cell.getColumn() + "宽度必须大于0");
                }
                cell.setWidth(cellWidth);
                cell.setX(startFrom);
                startFrom += cellWidth;
                int sum = IntStream.rangeClosed(1, row - 1).map(headRealRowHead::get).sum();
                cell.setY(sum + marginY);
            }
        });
    }

    /**
     * 生成表格各单元格内容对象
     *
     * @param tableRowContents            表格内容
     * @param actualLastHeadRowColumnCell 表头单元格
     * @param headRowSize                 表头行数
     * @param contentRowHeight            各行的行高关系
     * @return 表格各单元格内容对象
     */
    public static List<Cell> getTableContent(List<List<String>> tableRowContents, List<Cell> actualLastHeadRowColumnCell,
                                             int headRowSize, HashMap<Integer, Integer> contentRowHeight) {
        List<Cell> contents = new ArrayList<>();
        for (int i = 0; i < tableRowContents.size(); i++) {
            List<String> rowContent = tableRowContents.get(i);
            for (int j = 0; j < rowContent.size(); j++) {
                String cellContent = rowContent.get(j);
                Cell cell = new Cell();
                Cell lastHeadColumnCell = actualLastHeadRowColumnCell.get(j);
                cell.setRow(i + 1 + headRowSize);
                cell.setColumn(lastHeadColumnCell.getColumn());
                cell.setX(lastHeadColumnCell.getX());
                // 单元格纵坐标
                int sum = IntStream.rangeClosed(1, i).map(contentRowHeight::get).sum();
                int y = (lastHeadColumnCell.getY() + lastHeadColumnCell.getHeight()) + sum;
                cell.setY(y);
                cell.setWidth(lastHeadColumnCell.getWidth());
                cell.setTextAlign(true);
                cell.setHeight(contentRowHeight.get(i + 1));
                cell.setContent(cellContent);
                contents.add(cell);
            }
        }
        return contents;
    }

    /**
     * 按列合并单元格
     *
     * @param contents     单元格内容
     * @param mergeColumns 要合并的列
     * @param headCells    表头单元格
     */
    public static List<Cell> mergeCells(List<Cell> contents, int[] mergeColumns, List<Cell> headCells) {
        if (ArrayUtils.isEmpty(mergeColumns)) {
            contents.addAll(headCells);
            return contents;
        }
        if (CollectionUtils.isEmpty(contents)) {
            contents.addAll(headCells);
            return contents;
        }
        Map<Integer, List<Cell>> collect = contents.stream().collect(Collectors.groupingBy(Cell::getColumn));
        List<Cell> toDel = new ArrayList<>();
        for (int mergeColumn : mergeColumns) {
            List<Cell> cells = collect.get(mergeColumn).stream().sorted(Comparator.comparing(Cell::getRow))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(cells)) {
                for (int j = 0; j < cells.size() - 1; j++) {
                    Cell cell = cells.get(j);
                    for (int k = j + 1; k < cells.size(); k++) {
                        Cell cellToCompare = cells.get(k);
                        if (!org.apache.commons.lang3.StringUtils.equals(cell.getContent(), cellToCompare.getContent())) {
                            break;
                        }
                        cell.setHeight(cell.getHeight() + cellToCompare.getHeight());
                        toDel.add(cellToCompare);
                    }
                }
            }
        }
        ArrayList<Cell> cells = new ArrayList<>(CollectionUtils.subtract(contents, toDel));
        cells.addAll(headCells);
        return cells;
    }

    /**
     * 绘制表格
     *
     * @param tableCells 表格所有单元格
     * @param table      表格属性
     */
    public static BufferedImage starDrawTable(int headRow, List<Cell> tableCells, Table table, int tableHeight) throws IOException {
        int marginY = table.getMarginY();
        int marginX = table.getMarginX();
        Map<Integer, List<Cell>> allTableRows = tableCells.stream().collect(Collectors.groupingBy(Cell::getRow));
        int allRows = allTableRows.size();
        // 画布高度
        int imageHeight = tableHeight + marginY * 2;
        // 画布宽度
        int tableWidth = tableCells.stream().filter(cell -> cell.getRow() == 1).mapToInt(Cell::getWidth).sum();
        int imageWidth = tableWidth + marginX * 2;
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.white);
        // 默认白色背景
        graphics.fillRect(0, 0, imageWidth, imageHeight);
        IntStream.rangeClosed(1, allRows + 1).forEach(i ->
        {
            //表格最后一行画横线
            if (i == allRows + 1) {
                graphics.setColor(Color.black);
                // 获取第一行的所有单元格
                List<Cell> rowCells = allTableRows.get(1);
                for (Cell cell : rowCells) {
                    graphics.drawLine(cell.getX(), tableHeight + marginY, cell.getX() + cell.getWidth(), tableHeight + marginY);
                }
                return;
            }
            List<Cell> perRowCells = allTableRows.get(i);
            if (i <= headRow) {
                for (int h = 0; h < perRowCells.size(); h++) {
                    Cell cell = perRowCells.get(h);
                    //表头单元格填充背景色
                    graphics.setColor(table.getHeaderBackGroundColor());
                    graphics.fillRect(cell.getX(), cell.getY(), cell.getWidth(), cell.getHeight());
                    // 当前单元格是否为本行最后一个
                    boolean lastCellInRow = h == perRowCells.size() - 1;
                    coreMethod(graphics, table.getHeaderFont(), lastCellInRow, cell);
                }
                return;
            }
            for (int j = 0; j < perRowCells.size(); j++) {
                Cell cell = perRowCells.get(j);
                // 当前单元格是否为本行最后一个
                boolean lastCellInRow = j == perRowCells.size() - 1;
                coreMethod(graphics, table.getCellFont(), lastCellInRow, cell);
            }
        });
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//        Font font = graphics.getFont();
//        if (!font.de)
        Stroke basicStroke = new BasicStroke(imageWidth);
        graphics.setStroke(basicStroke);
        graphics.drawImage(image.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();
        return image;
    }

    /**
     * 绘制核心方法
     *
     * @param graphics      绘制类
     * @param font          字体信息
     * @param lastCellInRow 是否为行内最后一个单元格
     * @param cell          要绘制的单元格
     */
    private static void coreMethod(Graphics2D graphics, Font font, boolean lastCellInRow, Cell cell) {
        graphics.setColor(Color.black);
        //每个单元格左起横线
        graphics.drawLine(cell.getX(), cell.getY(), cell.getX() + cell.getWidth(), cell.getY());
        //每个单元格左起竖线
        graphics.drawLine(cell.getX(), cell.getY(), cell.getX(), cell.getY() + cell.getHeight());
        //单元格内容
        // 如果是水平居中,根据“字体”计算出实际起始坐标位置
        graphics.setFont(font);
        String content = org.apache.commons.lang3.StringUtils.defaultIfBlank(cell.getContent(), "-");
        String[] split = StringUtils.split(content, "\n");
        for (int i = 0; i < split.length; i++) {
            //1.计算单元格内容的横坐标，加1是为了防止文字紧贴在单元格上
            int contentX = cell.getX() + 1;
            String cellRowContent = split[i];
            if (cell.isTextAlign()) {
                // 初始执行比较耗时
                FontMetrics fontMetrics = graphics.getFontMetrics();
                int contentLen = fontMetrics.stringWidth(cellRowContent);
                contentX += (cell.getWidth() - contentLen) / 2;
            }
            // 2. 计算单元格纵坐标,默认居中(!!!注意：内容是从下向上从左向右渲染,所在在单元格的基础上又加了字体font.getSize())
            int startY = cell.getY() + font.getSize();
            // 单元格内第一行文字的纵坐标
            int cellFirstRowPosition = (cell.getHeight() - font.getSize() * (split.length)) / 2;
            // 偏移量（在行一行文字纵坐标的基础上进行累加），加1是为了防止每行文字粘在一起。
            int offset = (font.getSize() + 1) * i;
            int contentY = startY + cellFirstRowPosition + offset;
            // 3. 写入单元格内容
            graphics.drawString(cellRowContent, contentX, contentY);
        }
        //每行最后一个单元格的竖线
        if (lastCellInRow) {
            // 单元格最右的横坐标
            int cellRightX = cell.getX() + cell.getWidth();
            graphics.drawLine(cellRightX, cell.getY(), cellRightX, cell.getY() + cell.getHeight());
        }
    }
}
