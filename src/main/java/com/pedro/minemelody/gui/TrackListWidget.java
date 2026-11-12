package com.pedro.minemelody.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * Lista con scroll de canciones. La usamos dos veces: catálogo (izquierda) y playlist (derecha).
 * <p>
 * Es un componente "tonto" (presentacional): no sabe nada de playlists ni de discos.
 * Recibe filas ya armadas y avisa con callbacks cuando hacés clic o doble clic.
 */
public final class TrackListWidget extends ObjectSelectionList<TrackListWidget.Row> {

	private static final int ROW_HEIGHT = 14;
	private static final int RIGHT_TEXT_COLOR = 0xFF8A8F98;

	private final Consumer<Row> onClick;
	private final Consumer<Row> onDoubleClick;

	public TrackListWidget(Minecraft minecraft, int x, int y, int width, int height,
			Consumer<Row> onClick, Consumer<Row> onDoubleClick) {
		super(minecraft, width, height, y, ROW_HEIGHT);
		this.setX(x);
		this.onClick = onClick;
		this.onDoubleClick = onDoubleClick;
	}

	/** Reemplaza el contenido manteniendo seleccionada la fila con el mismo id (si sigue estando). */
	public void setRows(List<Row> rows, String selectedId) {
		replaceEntries(rows);
		rows.stream()
				.filter(row -> row.id().equals(selectedId))
				.findFirst()
				.ifPresent(row -> {
					setSelected(row);
					scrollToEntry(row);
				});
	}

	public String selectedId() {
		Row selected = getSelected();
		return selected == null ? null : selected.id();
	}

	public int size() {
		return children().size();
	}

	@Override
	public int getRowWidth() {
		return getWidth() - AbstractScrollArea.SCROLLBAR_WIDTH - 8;
	}

	@Override
	protected int scrollBarX() {
		return getRight() - AbstractScrollArea.SCROLLBAR_WIDTH;
	}

	/**
	 * Una fila: un id (para identificarla) y el texto con su color.
	 */
	public final class Row extends ObjectSelectionList.Entry<Row> {

		private final String id;
		private final String text;
		/** Texto alineado a la derecha (la duración), o null. */
		private final String rightText;
		private final int color;

		public Row(String id, String text, int color) {
			this(id, text, null, color);
		}

		public Row(String id, String text, String rightText, int color) {
			this.id = id;
			this.text = text;
			this.rightText = rightText;
			this.color = color;
		}

		public String id() {
			return id;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
			Font font = minecraft.font;
			int textY = getContentYMiddle() - font.lineHeight / 2;
			int available = getContentWidth();
			if (rightText != null) {
				int rightWidth = font.width(rightText);
				graphics.text(font, rightText, getContentRight() - rightWidth, textY, RIGHT_TEXT_COLOR);
				available -= rightWidth + 6;
			}
			String visible = text;
			if (font.width(visible) > available) {
				visible = font.plainSubstrByWidth(visible, available - font.width("...")) + "...";
			}
			graphics.text(font, visible, getContentX(), textY, color);
		}

		@Override
		public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
			onClick.accept(this);
			if (doubleClick) {
				onDoubleClick.accept(this);
			}
			return true; // true = esta fila queda seleccionada
		}

		@Override
		public Component getNarration() {
			return Component.literal(text);
		}
	}
}
