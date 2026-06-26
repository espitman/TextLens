(function () {
  const storageKey = "textlens-site-language";
  const preferred = localStorage.getItem(storageKey) || "en";

  function setLanguage(language) {
    const next = language === "fa" ? "fa" : "en";
    document.body.classList.toggle("lang-fa", next === "fa");
    document.body.classList.toggle("lang-en", next === "en");
    document.documentElement.lang = next === "fa" ? "fa" : "en";
    document.documentElement.dir = next === "fa" ? "rtl" : "ltr";
    localStorage.setItem(storageKey, next);

    document.querySelectorAll("[data-en][data-fa]").forEach((node) => {
      node.textContent = node.dataset[next];
    });

    document.querySelectorAll("[data-lang-toggle]").forEach((button) => {
      button.textContent = next === "fa" ? "EN" : "FA";
      button.setAttribute("aria-label", next === "fa" ? "Switch to English" : "Switch to Persian");
    });
  }

  setLanguage(preferred);

  document.querySelectorAll("[data-lang-toggle]").forEach((button) => {
    button.addEventListener("click", () => {
      setLanguage(document.body.classList.contains("lang-fa") ? "en" : "fa");
    });
  });

  const transitionMs = 240;
  const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  function isInternalPageLink(link) {
    const href = link.getAttribute("href");

    if (
      !href ||
      href.startsWith("#") ||
      href.startsWith("mailto:") ||
      href.startsWith("tel:") ||
      link.target === "_blank" ||
      link.hasAttribute("download")
    ) {
      return false;
    }

    const url = new URL(href, window.location.href);
    const current = new URL(window.location.href);
    const sameOrigin = url.origin === current.origin;
    const samePath = url.pathname === current.pathname;
    const onlyHashChange = sameOrigin && samePath && url.hash && url.hash !== current.hash;
    const isHtmlPage = url.pathname.endsWith("/") || url.pathname.endsWith(".html");

    return sameOrigin && isHtmlPage && !onlyHashChange;
  }

  document.querySelectorAll("a[href]").forEach((link) => {
    link.addEventListener("click", (event) => {
      if (
        event.defaultPrevented ||
        event.metaKey ||
        event.ctrlKey ||
        event.shiftKey ||
        event.altKey ||
        event.button !== 0 ||
        reduceMotion ||
        !isInternalPageLink(link)
      ) {
        return;
      }

      event.preventDefault();
      document.body.classList.add("is-page-leaving");

      window.setTimeout(() => {
        window.location.href = link.href;
      }, transitionMs);
    });
  });

  const observer = new IntersectionObserver(
    (entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        }
      });
    },
    { threshold: 0.14 },
  );

  document.querySelectorAll(".reveal").forEach((node) => observer.observe(node));
})();
