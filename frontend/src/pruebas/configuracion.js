import '@testing-library/jest-dom/vitest';

// jsdom no trae ResizeObserver, que usan los gráficos.
globalThis.ResizeObserver = globalThis.ResizeObserver || class {
  observe() {}
  unobserve() {}
  disconnect() {}
};
