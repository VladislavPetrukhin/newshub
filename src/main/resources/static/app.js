(() => {
  // авто-обновление раз в 5 минут (300000 ms)
  setTimeout(() => {
    const form = document.getElementById('refresh-form');
    if (form) form.submit();
  }, 300000);
})();
