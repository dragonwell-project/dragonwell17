/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
#include "precompiled.hpp"
#include "opto/c2_MacroAssembler.hpp"
#include "opto/c2_CodeStubs.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/stubRoutines.hpp"

#define __ masm.


int C2SlowFarJumpStub::max_size() const {
  return 2*4;
}

void C2SlowFarJumpStub::emit(C2_MacroAssembler& masm) {
  const Address get_ic_miss_stub_address = RuntimeAddress(SharedRuntime::get_ic_miss_stub());
  __ bind(entry());
  __ far_jump(get_ic_miss_stub_address);
  __ b(continuation());
}
