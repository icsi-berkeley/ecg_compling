package compling.gui.grammargui.util

trait Element {
   type E
   def parent: Element
   def content: E
}

trait Composite extends Element {
   def children: List[Element]
}
