package no.nav.foreldrepenger.datavarehus.tjeneste;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.datavarehus.domene.AnkeVurderingResultatDvh;

public class AnkeVurderingResultatDvhMapper {

    public AnkeVurderingResultatDvh map(AnkeVurderingResultatEntitet ankevurderingresultat) {
        return AnkeVurderingResultatDvh.builder()
            .medAnkeOmgjørÅrsak(ankevurderingresultat.getAnkeOmgjørÅrsak().getKode())
            .medAnkeBehandlingId(ankevurderingresultat.getAnkeResultat().getAnkeBehandlingId())
            .medAnkeVurdering(ankevurderingresultat.getAnkeVurdering().getKode())
            .medAnkeVurderingOmgjør(ankevurderingresultat.getAnkeVurderingOmgjør().getKode())
            .medErMerknaderMottatt(ankevurderingresultat.getErMerknaderMottatt())
            .medGjelderVedtak(ankevurderingresultat.getGjelderVedtak())
            .medErAnkerIkkePart(ankevurderingresultat.erAnkerIkkePart())
            .medErFristIkkeOverholdt(ankevurderingresultat.erFristIkkeOverholdt())
            .medErIkkeKonkret(ankevurderingresultat.erIkkeKonkret())
            .medErIkkeSignert(ankevurderingresultat.erIkkeSignert())
            .medErSubsidiartRealitetsbehandles(ankevurderingresultat.erSubsidiartRealitetsbehandles())
            .medOpprettetTidspunkt(ankevurderingresultat.getOpprettetTidspunkt())
            .build();
    }
}
