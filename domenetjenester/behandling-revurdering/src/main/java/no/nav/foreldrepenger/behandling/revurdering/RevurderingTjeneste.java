package no.nav.foreldrepenger.behandling.revurdering;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;

public interface RevurderingTjeneste {

    default Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet) {
        return opprettManuellRevurdering(fagsak, revurderingsÅrsak, enhet, null);
    }

    Behandling opprettManuellRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet, String opprettetAv);

    Behandling opprettAutomatiskRevurdering(Fagsak fagsak, BehandlingÅrsakType revurderingsÅrsak, OrganisasjonsEnhet enhet);

    void kopierAlleGrunnlagFraTidligereBehandling(Behandling original, Behandling ny);

    default void kopierAlleGrunnlagFraTidligereBehandlingTilUtsattSøknad(Behandling original, Behandling ny) {
        throw new IllegalStateException("Skal ikke kalles for ytelse type " + ny.getFagsakYtelseType().getKode());
    }

    Boolean kanRevurderingOpprettes(Fagsak fagsak);

    boolean erRevurderingMedUendretUtfall(Behandling behandling);

}
