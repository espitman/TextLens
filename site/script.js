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
