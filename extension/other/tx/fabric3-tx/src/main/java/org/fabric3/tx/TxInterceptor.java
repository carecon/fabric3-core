/*
 * Fabric3
 * Copyright (c) 2009-2015 Metaform Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Portions originally based on Apache Tuscany 2007
 * licensed under the Apache 2.0 license.
 */
package org.fabric3.tx;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.fabric3.spi.container.invocation.Message;
import org.fabric3.spi.container.wire.Interceptor;

/**
 * Implements transaction policy for a wire operation.
 */
public class TxInterceptor extends AbstractTxSupport implements Interceptor {
    private Interceptor next;

    public TxInterceptor(TransactionManager tm, TxAction action, TxMonitor monitor) {
        super(tm, action, monitor);
    }

    public Interceptor getNext() {
        return next;
    }

    public void setNext(Interceptor next) {
        this.next = next;
    }

    public Message invoke(Message message) {

        Transaction transaction = getTransaction();

        if (txAction == TxAction.BEGIN) {
            if (transaction == null) {
                begin();
            }
        } else if (txAction == TxAction.SUSPEND && transaction != null) {
            suspend();
        }

        Message ret;
        try {
            ret = next.invoke(message);
        } catch (RuntimeException e) {
            if (txAction == TxAction.BEGIN && transaction == null) {
                rollback();
            } else if (txAction == TxAction.SUSPEND && transaction != null) {
                monitor.resumeOnError(e);
                resume(transaction);
            }
            throw e;
        }

        if (txAction == TxAction.BEGIN && transaction == null && !ret.isFault()) {
            commit();
        } else if (txAction == TxAction.BEGIN && transaction == null && ret.isFault()) {
            rollback();
        } else if (txAction == TxAction.SUSPEND && transaction != null) {
            resume(transaction);
        }

        return ret;

    }
}
