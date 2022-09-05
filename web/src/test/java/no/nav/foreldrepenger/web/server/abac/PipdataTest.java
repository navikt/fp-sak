package no.nav.foreldrepenger.web.server.abac;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.mapper.json.DefaultJsonMapper;

class PipdataTest {

    @Test
    void roundtrip_pip() {
        var pip = new AbacPipDto(Set.of(new AktørId("0000000000000"), new AktørId("2222222222222")), AbacFagsakStatus.UNDER_BEHANDLING, AbacBehandlingStatus.UTREDES);
        var json = DefaultJsonMapper.toJson(pip);
        var roundtrip = DefaultJsonMapper.fromJson(json, AbacPipDto.class);
        assertThat(roundtrip).isEqualTo(pip);
        System.out.println(json);
    }

    @Test
    void roundtrip_compatible1() {
        var pip = new PseudoPip(Set.of("0000000000000", "2222222222222"), AbacFagsakStatus.UNDER_BEHANDLING.name(), AbacBehandlingStatus.UTREDES.name());
        var json = DefaultJsonMapper.toJson(pip);
        var roundtrip = DefaultJsonMapper.fromJson(json, AbacPipDto.class);
        assertThat(roundtrip.aktørIder().stream().map(AktørId::getId).collect(Collectors.toSet())).containsAll(pip.aktørIder());
        assertThat(roundtrip.fagsakStatus().name()).isEqualTo(pip.fagsakStatus());
        assertThat(roundtrip.behandlingStatus().name()).isEqualTo(pip.behandlingStatus());
    }

    @Test
    void roundtrip_compatible2() {
        var pip = new AbacPipDto(Set.of(new AktørId("0000000000000"), new AktørId("2222222222222")), AbacFagsakStatus.UNDER_BEHANDLING, AbacBehandlingStatus.UTREDES);
        var json = DefaultJsonMapper.toJson(pip);
        var roundtrip = DefaultJsonMapper.fromJson(json, PseudoPip.class);
        assertThat(pip.aktørIder().stream().map(AktørId::getId).collect(Collectors.toSet())).containsAll(roundtrip.aktørIder());
        assertThat(pip.fagsakStatus().name()).isEqualTo(roundtrip.fagsakStatus());
        assertThat(pip.behandlingStatus().name()).isEqualTo(roundtrip.behandlingStatus());
    }

    private static record PseudoPip(Set<String> aktørIder, String fagsakStatus, String behandlingStatus) {}

}
