package cn.paper_card.paper_card_tpa;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

class RequestContainer {

    private final TpRequest[] requests;

    RequestContainer() {
        this.requests = new TpRequest[256];
        Arrays.fill(this.requests, null);
    }

    boolean add(@NotNull TpRequest request) {
        synchronized (this) {
            for (int i = 0; i < this.requests.length; ++i) {
                if (this.requests[i] == null) {
                    this.requests[i] = request;
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable TpRequest removeBySrcPlayer(@NotNull Player srcPlayer) {
        synchronized (this) {
            for (int i = 0; i < this.requests.length; ++i) {
                final TpRequest request = requests[i];

                if (request == null) continue;

                if (request.srcPlayer().getUniqueId().equals(srcPlayer.getUniqueId())) {
                    this.requests[i] = null;
                    return request;
                }
            }
        }
        return null;
    }


    @Nullable TpRequest remove(@NotNull Player srcPlayer, @NotNull Player destPlayer) {
        synchronized (this) {
            for (int i = 0; i < this.requests.length; ++i) {
                if (this.requests[i] == null) continue;

                final TpRequest request = this.requests[i];
                if (request.srcPlayer().getUniqueId().equals(srcPlayer.getUniqueId())) {
                    if (request.destPlayer().getUniqueId().equals(destPlayer.getUniqueId())) {
                        this.requests[i] = null;
                        return request;
                    }
                }
            }
        }
        return null;
    }
}
