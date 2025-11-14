/*
 * Copyright 2024 Marc Nuri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package helm

import (
	"bytes"
	"fmt"
	"os"
	"strings"
	"testing"
)

func TestNewDebugCapture(t *testing.T) {
	t.Run("with enabled=false should not capture output", func(t *testing.T) {
		dc := NewDebugCapture(false)
		if dc.enabled {
			t.Error("Expected debug capture to be disabled")
		}
	})
	t.Run("with enabled=true should capture stdout", func(t *testing.T) {
		dc := NewDebugCapture(true)
		fmt.Println("test output")
		buf := &bytes.Buffer{}
		dc.StopAndAppendTo(buf)
		if !strings.Contains(buf.String(), "test output") {
			t.Errorf("Expected captured output to contain 'test output', got %s", buf.String())
		}
	})
	t.Run("with enabled=true should capture stderr", func(t *testing.T) {
		dc := NewDebugCapture(true)
		_, _ = fmt.Fprintln(os.Stderr, "test error")
		buf := &bytes.Buffer{}
		dc.StopAndAppendTo(buf)
		if !strings.Contains(buf.String(), "test error") {
			t.Errorf("Expected captured output to contain 'test error', got %s", buf.String())
		}
	})
	t.Run("with enabled=true should capture both stdout and stderr", func(t *testing.T) {
		dc := NewDebugCapture(true)
		fmt.Println("stdout message")
		_, _ = fmt.Fprintln(os.Stderr, "stderr message")
		buf := &bytes.Buffer{}
		dc.StopAndAppendTo(buf)
		if !strings.Contains(buf.String(), "stdout message") {
			t.Errorf("Expected captured output to contain 'stdout message', got %s", buf.String())
		}
		if !strings.Contains(buf.String(), "stderr message") {
			t.Errorf("Expected captured output to contain 'stderr message', got %s", buf.String())
		}
	})
}

func TestDebugCaptureStopAndAppendTo(t *testing.T) {
	t.Run("should append to existing buffer content", func(t *testing.T) {
		dc := NewDebugCapture(true)
		_, _ = fmt.Println("new content")
		buf := &bytes.Buffer{}
		buf.WriteString("existing content")
		dc.StopAndAppendTo(buf)
		if !strings.Contains(buf.String(), "existing content") {
			t.Errorf("Expected buffer to contain existing content, got %s", buf.String())
		}
		if !strings.Contains(buf.String(), "new content") {
			t.Errorf("Expected buffer to contain new content, got %s", buf.String())
		}
	})
	t.Run("should not append when disabled", func(t *testing.T) {
		dc := NewDebugCapture(false)
		_, _ = fmt.Println("should not be captured")
		buf := &bytes.Buffer{}
		dc.StopAndAppendTo(buf)
		if buf.Len() > 0 {
			t.Errorf("Expected buffer to be empty, got %s", buf.String())
		}
	})
	t.Run("should not append when cleanup already executed", func(t *testing.T) {
		dc := NewDebugCapture(true)
		_, _ = fmt.Println("first call")
		buf := &bytes.Buffer{}
		dc.StopAndAppendTo(buf)
		originalLen := buf.Len()
		dc.StopAndAppendTo(buf)
		if buf.Len() != originalLen {
			t.Errorf("Expected buffer length to remain %d after second call, got %d", originalLen, buf.Len())
		}
	})
	t.Run("should restore original stdout and stderr", func(t *testing.T) {
		originalStdout := os.Stdout
		originalStderr := os.Stderr
		dc := NewDebugCapture(true)
		buf := &bytes.Buffer{}
		dc.StopAndAppendTo(buf)
		if os.Stdout != originalStdout {
			t.Error("Expected stdout to be restored to original")
		}
		if os.Stderr != originalStderr {
			t.Error("Expected stderr to be restored to original")
		}
	})
}
