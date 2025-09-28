package com.github.kyrobbins.common.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RsaUtilityIT {

    @Test
    void createPrivateKey() throws Exception{
        String keyString = "-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCmk88eRuDhttBC\n" +
                "5W1UES+o5x7VFbJGB3xCXowGZeBvJsI1QVCblTy/BjJ1ntVMtUO/NPfdyjAQ9n6k\n" +
                "yGV6YB30Uu8uML/vsdn00PMCCh8ltz4mex5LPcL8yxCP/DGnhOdvDXq9q+E/TP+J\n" +
                "11ld60BMagSwC5OC0c+3wa+Xjcy4/Q3eNNTuqV2Grk48f9nFAmZBEGaO9vwlTTB6\n" +
                "uSNYWU6YAg6YfOrKnBZLVTRs99zjaEtITmZfMqWJazKp9nUPKjPCwdrzOGrcyx33\n" +
                "EwrVYfZF4D8eaGsFFod64kv8q2Wi6aUSH+am6dEHh4lNbbaNOnQ2JliV6r1QLvUv\n" +
                "6/o4AxMbAgMBAAECggEAEmK3FhbppFAavX94KTK6aXCVERzTb/JMj0DDQMOG2Y3U\n" +
                "Gq+qV3nJ2iWzdRMCZPSO1TBh+UtjMcQbJ1HtDWxFch2jQ5xi2vBs554WbE+0r2OB\n" +
                "yxqEtfZLqr45BkNmMaC0MKgyt3OcqXF0tQCdXwXjs2tyOAIXFnXU8OnCWT5v7azm\n" +
                "76+Z1R7IIK53mfibZtzmszbcR+btnbVzmn4weF3xFw0Rux6ZxYecCVMXkHDiXOLP\n" +
                "6w0p762x7gFGTlsNZk4XZrfmorGAkRZ8OlIFMQECEo32w/k/paGr5rcM91z+6X7L\n" +
                "MZUri5bFnv1deSRu0OPXOAHFHNMKRaPzBdaE0gJMyQKBgQDZ6eslrEyvsUi1QNQ6\n" +
                "76giDRgVIriMZ7kL3F8NZSZhWyHPmVTiLoJi/dphbFc0lD+zYRKfninmfqYajTVv\n" +
                "aYMaBYyeWNA2IBIl6DZ+9nQs2vhdbt2Y7KxbxfknSbVNaYMDib+G68iL4xcYZgWK\n" +
                "qCnfcQsciT2GGXOEx+5+vObi2QKBgQDDsPSRoRTqTNr/MbR0eLlS+YCJqtfNYUbf\n" +
                "qVANwUh/vRdtnhXLIZTSrdOx1wuLfdC0/9hPi5/qJB7ICSuOqv9Af7ShpAvTmo0A\n" +
                "mBk9x6uDv5hLOTri1umhPmx498M14dGg73mV3q/Dmjh4viT7u+rVON7Rbl4mJiTK\n" +
                "v72LCMIFEwKBgB5e+cUAZJfAD7AjXvDv4fEM/iRD+JCpDCNZDW5igJzlBKtYZXo4\n" +
                "nTeKxdr6LJVJbC9mAiB4/MIBOMlOkPn8LKd9yoFvQ2Wnxp+944qCcuKliiQLhZsA\n" +
                "sW0BBJ6zKS+m2vmtQyJczlCgz+E4puESWvOnX7MUdZyxA2aoLtc/ILbRAoGALgyw\n" +
                "IKzlPRWE21SZAGeARNRNuReo3L2tbehr3DDFnLmkj7kJq9llKgaZsFnIb7TVriIe\n" +
                "hul2YiTJ0YzG4TXABy+GFBuEZPETiTZBilY4ODKX5eu7vbGRHM6RvA2htEZFjDeO\n" +
                "RwQ2HDNoYpFsJojcA/z0AhiUsyl6svNX6SXQcl0CgYEAmdN0i3nAN8zT0Up+n4Cj\n" +
                "lovfV7i2XKptVVfz0w/ql7YDOLDjpCezSArCaZwhaAjEdBTKlwyMTXjmWD8xiM51\n" +
                "LPyUobBpssp2YopaaZBrZd1li+NaOdZluz1nlCqEhvLLxk7fpxITu2vwvm1D6Njd\n" +
                "HTtsr1B9Tct1mESKaV4qQII=\n" +
                "-----END PRIVATE KEY-----";

        RsaKeyUtility utility = new RsaKeyUtility();

        assertDoesNotThrow(() -> utility.createPrivateKey(keyString));
    }

    @Test
    void createPublicKey() throws Exception{
        String keyString = "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAppPPHkbg4bbQQuVtVBEv\n" +
                "qOce1RWyRgd8Ql6MBmXgbybCNUFQm5U8vwYydZ7VTLVDvzT33cowEPZ+pMhlemAd\n" +
                "9FLvLjC/77HZ9NDzAgofJbc+JnseSz3C/MsQj/wxp4Tnbw16vavhP0z/iddZXetA\n" +
                "TGoEsAuTgtHPt8Gvl43MuP0N3jTU7qldhq5OPH/ZxQJmQRBmjvb8JU0werkjWFlO\n" +
                "mAIOmHzqypwWS1U0bPfc42hLSE5mXzKliWsyqfZ1DyozwsHa8zhq3Msd9xMK1WH2\n" +
                "ReA/HmhrBRaHeuJL/KtloumlEh/mpunRB4eJTW22jTp0NiZYleq9UC71L+v6OAMT\n" +
                "GwIDAQAB\n" +
                "-----END PUBLIC KEY-----";

        RsaKeyUtility utility = new RsaKeyUtility();

        assertDoesNotThrow(() -> utility.createPublicKey(keyString));
    }

}
