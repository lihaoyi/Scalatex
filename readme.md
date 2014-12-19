


<h1>Scalatex 0.1.0</h1>
  <i>Programmable Documents in Scala</i>

  <div><pre><code>@div(id:=&quot;my-div&quot;)
  @h1
    Hello World!
  @p
    I am Cow
    Hear me moo
    I weigh @b{twice} as much as you
    And I look good on the barbecue</code></pre><pre><code>&lt;div id=&quot;my-div&quot;&gt;
  &lt;h1&gt;Hello World!&lt;/h1&gt;
  &lt;p&gt;
    I am Cow
    Hear me moo
    I weigh &lt;b&gt;twice&lt;/b&gt; as much as you
    And I look good on the barbecue
  &lt;/p&gt;
&lt;/div&gt;</code></pre></div>

  <p>
    Scalatex is a language for generating rich HTML documents in Scala. It lets you write your HTML in a Jade-like DSL which then gets compiled to HTML. In Scalatex, everything is part of the output document, except for tokens marked with an <code>@</code> sign. These correspond to HTML tags (provided by Scalatags), values you want to interpolate, control flow statements, function calls (or definitions!), basically anything that isn't plain text.
</p>
  <p>
    Scalatex allows you to DRY up your documents the same way you DRY your code. Unlike most templating languages, Scalatex is a thin shim on top of the Scala programming language. This lets you use the full power of the Scala programming language to build your documents, avoiding the copy-and-paste duplication that often happens in less-powerful document-generation languages.
</p>
  <h2>Getting Started</h2>
    <p>
      To get started with Scalatex, add the following to your <code>project/build.sbt</code>:
</p>
    <pre>
      addSbtPlugin(&quot;com.lihaoyi&quot; %% &quot;scalatex&quot; % &quot;0.1.0&quot;)
</pre>
    <p>
      And the following to your project in your <code>build.sbt</code>:</p>
    <pre>
      project.settings(scalatex.SbtPlugin.projectSettings:_*)
</pre>
    <p>
      To begin with, let's create a file in <code>src/main/scalatex/Hello.scalatex</code>
</p>
    <pre><code>
@div
  Hello World

  @h1
    I am a cow!</code></pre>

    <p>
      Next, you can simply call</p>
    <pre>
      Hello().render
</pre>
    <p>
      And it'll print out
</p>
    <pre>
      &lt;div&gt;
        Hello World

        &lt;h1&gt;
          I am a cow!&lt;/h1&gt;&lt;/div&gt;
</pre>
    <p>
      There we have it, your first Scalatex document! You can put this on gh-pages, use it on a website, or where-ever you want.
</p>
  <h2>What Scalatex Does</h2>

    <p>
      Scalatex converts every <code>.scalatex</code> file in its source folder into a corresponding Scala <code>object</code>. These objects have an <code>apply</code> method which returns a Scalatags <code>Frag</code>, which you can then call <code>.render</code> on to give you a HTML string. You can also do other things with the Scalatags <code>Frag</code>, but to learn more about it and Scalatags in general take a look at the Scalatags documentation.
</p>
    <p>
      Inside each Scalatex file, <code>@</code>-escaped things correspond to Scala keywords or names that are currently in scope. Apart from keywords, only <code>scalatags.Text.all._</code> is imported by default. This brings in all the HTML tags that we used above to build those HTML fragments. However, you can also <code>@import</code> whatever other names you want, or refer to them fully qualified.
</p>
  <h2>Exploring Scalatex</h2>
    <div><pre><code>@div
  @h1(&quot;Hello World&quot;)
  @h2{I am Cow}
  @p
    Hear me moo
    I weigh @b{twice} as much as you
    And I look good on the barbecue</code></pre><pre><code>&lt;div&gt;
  &lt;h1&gt;Hello World&lt;/h1&gt;
  &lt;h2&gt;I am Cow&lt;/h2&gt;
  &lt;p&gt;
    Hear me moo
    I weigh &lt;b&gt;twice&lt;/b&gt; as much as you
    And I look good on the barbecue
  &lt;/p&gt;
&lt;/div&gt;</code></pre></div>

    <p>
      Superficially, Scalatex does a small number of translations to convert the <code>.scalatex</code> document into Scala code:
</p>
    <ul>
      <li>
        @-escapes followed by identation (like <code>@div</code> above) are passed all the subsequently-indented lines as children.</li>
      <li>
        @-escapes followed by curly braces (like <code>@h2</code> above) are passed everything inside the curly braces as children</li>
      <li>
        @-escapes followed by parentheses (like <code>@h1</code> above) are passed the contents of the parentheses as a Scala expression.</li>
      <li>
        Everything outside of a set of parentheses, that isn't an @-escape, is treated as a string.
</li></ul>
    <p>
      This accounts for the variation in syntax that you see above. In general, you almost always want to use identation-based blocks to delimit large sections of the document, only falling back to curly braces for one-line or one-word tags like <code>@h2</code> or <code>@b</code> above.
</p>
    <h3>Loops</h3>
      <div><pre><code>@ul
  @for(x &lt;- 0 until 3)
    @li
      lol @x</code></pre><pre><code>@ul
   @for(x &lt;- 0 until 3){@li{lol @x}}</code></pre><pre><code>&lt;ul&gt;
  &lt;li&gt;lol 0&lt;/li&gt;
  &lt;li&gt;lol 1&lt;/li&gt;
  &lt;li&gt;lol 2&lt;/li&gt;
&lt;/ul&gt;</code></pre></div>

      <p>
        Scalatex supports for-loops, as shown above. Again, everything inside the parentheses are an arbitrary Scala expression. Here we can see it binding the <code>x</code> value, which is then used in the body of the loop as <code>@x</code> to splice it into the document.</p>
      <p>
        In general, there are always two contexts to keep track of:
</p>
      <ul>
        <li>
          Scalatex: strings are raw <code>text</code>, but variables are escaped as <code>@x</code></li>
        <li>
          Scala: strings are enclosed e.g. <code>&quot;text&quot;</code>, but variables are raw <code>x</code>
</li></ul>
      <p>
        The value of strings or variables is completely identical in both contexts; it's only the syntax that differs.
</p>
    <h3>Conditionals</h3>
      <div><pre><code>@ul
  @for(x &lt;- 0 until 5)
    @li
      @if(x % 2 == 0)
        @b{lol} @x
      @else
        @i{lol} @x</code></pre><pre><code>&lt;ul&gt;
  &lt;li&gt;&lt;b&gt;lol&lt;/b&gt; 0&lt;/li&gt;
  &lt;li&gt;&lt;i&gt;lol&lt;/i&gt; 1&lt;/li&gt;
  &lt;li&gt;&lt;b&gt;lol&lt;/b&gt; 2&lt;/li&gt;
  &lt;li&gt;&lt;i&gt;lol&lt;/i&gt; 3&lt;/li&gt;
  &lt;li&gt;&lt;b&gt;lol&lt;/b&gt; 4&lt;/li&gt;
&lt;/ul&gt;</code></pre></div>

      <p>
        Scalatex supports if-else statements, that behave as you'd expect. Here we're using one in conjunction with a loop to alternate the formatting of different items in a list.
</p>
    <h3>Functions</h3>
      <div><pre><code>@span
  The square root of 9.0 is @math.sqrt(9.0)</code></pre><pre><code>&lt;span&gt;The square root of 9.0 is 3.0&lt;/span&gt;</code></pre></div>

      <p>
        Apart from splicing values into the document, you can also call functions, such as <code>math.sqrt</code> here.
</p>
    <h3>Interpolations</h3>
      <div><pre><code>@span
  1 + 2 is @(1 + 2)</code></pre><pre><code>@span
  1 + 2 is @{1 + 2}</code></pre><pre><code>&lt;span&gt;1 + 2 is 3&lt;/span&gt;</code></pre></div>

      <p>
        You can also splice the result of arbitrary chunks of code within a Scalatex document. Using parens <code>()</code> lets you splice a single expression, whereas using curlies <code>{</code>} lets you splice a block which can contain multiple statements.
</p>
      <p>
        Blocks can span many lines:
</p>
      <div><pre><code>@div
  1 + 2 is @{
    val x = &quot;1&quot;
    val y = &quot;2&quot;
    println(s&quot;Adding $x and $y&quot;)
    x.toInt + y.toInt
  }</code></pre><pre><code>&lt;div&gt;
  1 + 2 is 3
&lt;/div&gt;</code></pre></div>

    <h3>External Definitions</h3>
      <p>
        You can use imports to bring things into scope, so you don't need to keep referring to them by their full name:
</p>
      <div><pre><code>@import scala.math._
@ul
  @for(i &lt;- -2 to 2)
    @li
      @abs(i)</code></pre><pre><code>&lt;ul&gt;
  &lt;li&gt;2&lt;/li&gt;
  &lt;li&gt;1&lt;/li&gt;
  &lt;li&gt;0&lt;/li&gt;
  &lt;li&gt;1&lt;/li&gt;
  &lt;li&gt;2&lt;/li&gt;
&lt;/ul&gt;</code></pre></div>
      <p>
        Since you can splice the value of any Scala expressions, of course you can splice the value of values that you defined yourself:
</p>
      <pre><code>object Stuff {
  object omg {
    def wrap(s: Frag*): Frag = Seq[Frag](&quot;...&quot;, s, &quot;...&quot;)
  }
  def str = &quot;hear me moo&quot;
}</code></pre>


      <div><pre><code>@import Stuff._
@omg.wrap
  i @b{am} cow @str</code></pre><pre><code>...i &lt;b&gt;am&lt;/b&gt; cow hear me moo...</code></pre></div>