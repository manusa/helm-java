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
	"io"
	"os"
	"strings"
	"sync"
)

// DebugCapture captures stdout and stderr output for debug purposes
type DebugCapture struct {
	enabled         bool
	capturedOut     strings.Builder
	capturedErr     strings.Builder
	originalStdout  *os.File
	originalStderr  *os.File
	outWriter       *os.File
	errWriter       *os.File
	wg              sync.WaitGroup
	cleanupExecuted bool
}

// NewDebugCapture creates a new debug capture instance and starts capturing if enabled
func NewDebugCapture(enabled bool) *DebugCapture {
	dc := &DebugCapture{enabled: enabled}
	if !enabled {
		return dc
	}

	// Save original stdout/stderr
	dc.originalStdout = os.Stdout
	dc.originalStderr = os.Stderr

	// Create pipes
	outReader, outWriter, _ := os.Pipe()
	errReader, errWriter, _ := os.Pipe()
	dc.outWriter = outWriter
	dc.errWriter = errWriter

	// Redirect stdout/stderr
	os.Stdout = outWriter
	os.Stderr = errWriter

	// Start reading from pipes
	dc.wg.Add(2)
	go func() {
		defer dc.wg.Done()
		_, _ = io.Copy(&dc.capturedOut, outReader)
	}()
	go func() {
		defer dc.wg.Done()
		_, _ = io.Copy(&dc.capturedErr, errReader)
	}()

	return dc
}

// StopAndAppendTo stops capturing, appends the captured stdout/stderr to the provided buffer, and cleans up
func (dc *DebugCapture) StopAndAppendTo(buf *bytes.Buffer) {
	if !dc.enabled || dc.cleanupExecuted {
		return
	}
	dc.cleanupExecuted = true

	// Close write ends to signal EOF
	_ = dc.outWriter.Close()
	_ = dc.errWriter.Close()

	// Wait for readers to finish
	dc.wg.Wait()

	// Restore original stdout/stderr
	os.Stdout = dc.originalStdout
	os.Stderr = dc.originalStderr

	// Append captured output to buffer
	if dc.capturedOut.Len() > 0 {
		if buf.Len() > 0 {
			buf.WriteString("\n")
		}
		buf.WriteString(dc.capturedOut.String())
	}
	if dc.capturedErr.Len() > 0 {
		if buf.Len() > 0 {
			buf.WriteString("\n")
		}
		buf.WriteString(dc.capturedErr.String())
	}
}
