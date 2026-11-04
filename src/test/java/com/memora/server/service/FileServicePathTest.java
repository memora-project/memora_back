package com.memora.server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * BE-P0-01: photoUrl이 업로드 폴더 밖 파일을 가리키지 못하는지 확인.
 */
class FileServicePathTest {

    @TempDir
    Path uploadDir;

    private Path resolve(String photoUrl) {
        return FileService.resolveUploadedFile(uploadDir.toString(), photoUrl);
    }

    @Test
    void 정상_업로드_URL은_업로드_폴더_안의_파일로_변환된다() {
        Path expected = uploadDir.toAbsolutePath().normalize()
                .resolve("550e8400-e29b-41d4-a716-446655440000.jpg");
        assertEquals(expected, resolve("/uploads/550e8400-e29b-41d4-a716-446655440000.jpg"));
    }

    @Test
    void 접두어_없는_파일명도_기존처럼_허용된다() {
        Path expected = uploadDir.toAbsolutePath().normalize().resolve("abc.png");
        assertEquals(expected, resolve("abc.png"));
    }

    @Test
    void 상위_폴더_탈출은_차단된다() {
        assertNull(resolve("/uploads/../.env"));
        assertNull(resolve("/uploads/../../etc/passwd"));
        assertNull(resolve("../build.gradle"));
        assertNull(resolve("/uploads/..\\..\\build.gradle"));
        assertNull(resolve("/uploads/sub/../../x.jpg"));
    }

    @Test
    void 절대_경로는_차단된다() {
        assertNull(resolve("/uploads//etc/passwd"));
        assertNull(resolve("/etc/passwd"));
        assertNull(resolve("/uploads/C:/Windows/win.ini"));
        assertNull(resolve("C:\\Windows\\win.ini"));
        assertNull(resolve(uploadDir.getParent().resolve("secret.txt").toString()));
    }

    @Test
    void 빈_값이나_폴더_자체는_null() {
        assertNull(resolve(null));
        assertNull(resolve(""));
        assertNull(resolve("   "));
        assertNull(resolve("/uploads/"));
        assertNull(resolve("/uploads/."));
    }
}
