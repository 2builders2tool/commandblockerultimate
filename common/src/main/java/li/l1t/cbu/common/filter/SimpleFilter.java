/*
 * Command Blocker Ultimate
 * Copyright (C) 2014-2018 Philipp Nowak / Literallie (l1t.li)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package li.l1t.cbu.common.filter;

import com.google.common.base.Preconditions;
import li.l1t.cbu.common.filter.action.FilterAction;
import li.l1t.cbu.common.filter.config.FilterConfiguration;
import li.l1t.cbu.common.filter.criterion.CriteriaList;
import li.l1t.cbu.common.filter.dto.CommandLine;
import li.l1t.cbu.common.filter.dto.Completable;
import li.l1t.cbu.common.filter.result.FilterOpinion;
import li.l1t.cbu.common.platform.SenderAdapter;

import javax.annotation.Nonnull;

/**
 * A command filter with individual configuration that has a defined set of criteria.
 *
 * @author <a href="https://l1t.li/">Literallie</a>
 * @since 2018-02-22
 */
public class SimpleFilter extends CriteriaList implements Filter {
    private final FilterConfiguration configuration;

    public SimpleFilter(FilterConfiguration configuration) {
        super(Preconditions.checkNotNull(configuration, "configuration").getDefaultOpinion());
        this.configuration = configuration;
    }

    @Nonnull
    @Override
    public FilterConfiguration config() {
        return configuration;
    }

    @Nonnull
    @Override
    public FilterOpinion processExecution(CommandLine commandLine, SenderAdapter sender) {
        if (!config().doesPreventExecution()) {
            return FilterOpinion.NONE;
        }
        return processAction(config().getExecutionAction(), commandLine, sender);
    }

    @Nonnull
    private FilterOpinion processAction(FilterAction handler, CommandLine commandLine, SenderAdapter sender) {
        Preconditions.checkNotNull(sender, "sender");
        Preconditions.checkNotNull(commandLine, "commandLine");
        FilterOpinion result = super.process(commandLine);
        if (sender.hasPermission(config().getBypassPermission())) {
            handler.onBypass(commandLine, sender);
            return FilterOpinion.NONE;
        } else if (result == FilterOpinion.DENY) {
            handler.onDenial(commandLine, sender);
        }
        return result;
    }

    @Nonnull
    @Override
    public FilterOpinion processTabComplete(Completable completable) {
        Preconditions.checkNotNull(completable, "completable");
        if (!config().doesPreventTabComplete()) {
            return FilterOpinion.NONE;
        }
        return completable.findMergedCommand()
                .map(line -> processAction(config().getTabCompleteAction(), line, completable.getSender()))
                .orElse(FilterOpinion.NONE);
    }

    @Override
    public FilterOpinion getDefaultOpinion() {
        return config().getDefaultOpinion();
    }

    @Override
    public void setDefaultOpinion(FilterOpinion defaultOpinion) {
        Preconditions.checkNotNull(defaultOpinion, "defaultOpinion");
        config().setDefaultOpinion(defaultOpinion);
    }
}
