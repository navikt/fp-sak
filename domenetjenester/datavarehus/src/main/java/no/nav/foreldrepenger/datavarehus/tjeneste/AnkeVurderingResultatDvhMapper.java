package no.nav.foreldrepenger.datavarehus.tjeneste;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurdering;
import no.nav.foreldrepenger.behandlingslager.behandling.anke.AnkeVurderingResultatEntitet;
import no.nav.foreldrepenger.datavarehus.domene.AnkeVurderingResultatDvh;

class AnkeVurderingResultatDvhMapper {

    private AnkeVurderingResultatDvhMapper() {
    }

    static AnkeVurderingResultatDvh map(AnkeVurderingResultatEntitet ankevurderingresultat) {
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
            .medTrygderettVurdering(ankevurderingresultat.getTrygderettVurdering().getKode())
            .medTrygderettOmgjørÅrsak(ankevurderingresultat.getTrygderettOmgjørÅrsak().getKode())
            .medTrygderettVurderingOmgjør(ankevurderingresultat.getTrygderettVurderingOmgjør().getKode())
            .medTrygderettOversendtDato(ankevurderingresultat.getSendtTrygderettDato())
            .medTrygderettKjennelseDato(getKjennelseDato(ankevurderingresultat))
            .build();
    }

    private static LocalDate getKjennelseDato(AnkeVurderingResultatEntitet avr) {
        if (AnkeVurdering.UDEFINERT.equals(avr.getTrygderettVurdering()))
            return null;
        return avr.getOpprettetTidspunkt() != null ? avr.getOpprettetTidspunkt().toLocalDate() : LocalDate.now();
    }
}
