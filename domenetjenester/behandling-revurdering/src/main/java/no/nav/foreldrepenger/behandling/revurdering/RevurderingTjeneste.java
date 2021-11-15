package no.nav.foreldrepenger.behandling.revurdering;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

public interface RevurderingTjeneste {

    Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet);

    Behandling opprettAutomatiskRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet);

    default Behandling opprettAutomatiskRevurderingMultiÅrsak(Fagsak fagsak, List<BehandlingÅrsakType> revurderingsÅrsaker, OrganisasjonsEnhet enhet) {
        throw new IllegalStateException("Skal ikke kalles for ytelse type " + fagsak.getYtelseType().getKode());
    }

    void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny);

    default void kopierAlleGrunnlagFraTidligereBehandlingTilUtsattSøknad(Behandling original, Behandling ny) {
        throw new IllegalStateException("Skal ikke kalles for ytelse type " + ny.getFagsakYtelseType().getKode());
    }

    Boolean kanRevurderingOpprettes(Fagsak fagsak);

    boolean erRevurderingMedUendretUtfall(Behandling behandling);

}
