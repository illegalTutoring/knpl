/// <reference types="vite/client" /> //

interface ImportMetaEnv {
    readonly VITE_APP_API_URL: string; //이부분을 수정해준다. 위에 적은 변수의 타입을 적어준다
    // 다른 환경 변수들에 대해 아래에 위에처럼 타입을 지정해준다
  }
  
  interface ImportMeta {
    readonly env: ImportMetaEnv;
  }

