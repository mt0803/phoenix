/*******************************************************************************
 * Copyright (c) 2013, Salesforce.com, Inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *     Neither the name of Salesforce.com nor the names of its contributors may 
 *     be used to endorse or promote products derived from this software without 
 *     specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package phoenix.query;

import java.sql.SQLException;
import java.util.List;

import phoenix.compile.ExplainPlan;
import phoenix.compile.RowProjector;
import phoenix.iterate.ResultIterator;
import phoenix.schema.tuple.Tuple;

import com.google.common.collect.Lists;


/**
 * Wrapper for ResultScanner to enable joins and aggregations to be composable.
 *
 * @author jtaylor
 * @since 0.1
 */
public class WrappedScanner implements Scanner {
    public static final int DEFAULT_ESTIMATED_SIZE = 10 * 1024; // 10 K

    private final ResultIterator scanner;
    private final RowProjector projector;
    private final int maxRows;
    // TODO: base on stats
    private final int estimatedSize = DEFAULT_ESTIMATED_SIZE;

    public WrappedScanner(ResultIterator scanner, RowProjector projector) {
        this(scanner, projector, 0);
    }

    public WrappedScanner(ResultIterator scanner, RowProjector projector, int maxRows) {
        this.scanner = scanner;
        this.projector = projector;
        this.maxRows = maxRows;
    }

    @Override
    public int getEstimatedSize() {
        return estimatedSize;
    }
    
    @Override
    public ResultIterator iterator() {
        if (maxRows == 0) {
            return scanner;
        }
        return new ResultIterator() {
            private int rowCount;
            @Override
            public void close() throws SQLException {
                scanner.close();
            }

            @Override
            public Tuple next() throws SQLException {
                if (rowCount++ >= maxRows) {
                    return null;
                }
                return scanner.next();
            }

            @Override
            public void explain(List<String> planSteps) {
                scanner.explain(planSteps);
            }
        };
    }

    @Override
    public RowProjector getProjection() {
        return projector;
    }
    
    @Override
    public ExplainPlan getExplainPlan() {
        List<String> planSteps = Lists.newArrayListWithExpectedSize(5);
        scanner.explain(planSteps);
        return new ExplainPlan(planSteps);
    }
}