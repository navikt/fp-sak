package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.søknad.v3;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.vedtak.felles.xml.soeknad.engangsstoenad.v3.Engangsstønad;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.Medlemskap;
import no.nav.vedtak.felles.xml.soeknad.felles.v3.OppholdUtlandet;
import no.nav.vedtak.felles.xml.soeknad.foreldrepenger.v3.Foreldrepenger;
import no.nav.vedtak.felles.xml.soeknad.svangerskapspenger.v1.Svangerskapspenger;

public class MedlemskapOversetter {

    private final MedlemskapRepository medlemskapRepository;

    public MedlemskapOversetter(MedlemskapRepository medlemskapRepository) {
        this.medlemskapRepository = medlemskapRepository;
    }

    void byggMedlemskap(SøknadWrapper skjema, Long behandlingId, LocalDate forsendelseMottatt) {
        Medlemskap medlemskap;
        var omYtelse = skjema.getOmYtelse();
        var mottattDato = skjema.getSkjema().getMottattDato();
        var oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder().medOppholdNå(true)
            .medOppgittDato(forsendelseMottatt);

        if (omYtelse instanceof Engangsstønad engangsstønad) {
            medlemskap = engangsstønad.getMedlemskap();
        } else if (omYtelse instanceof Foreldrepenger foreldrepenger) {
            medlemskap = foreldrepenger.getMedlemskap();
        } else if (omYtelse instanceof Svangerskapspenger svangerskapspenger) {
            medlemskap = svangerskapspenger.getMedlemskap();
        } else {
            throw new IllegalStateException("Ytelsestype er ikke støttet");
        }
        Boolean iNorgeVedFoedselstidspunkt = medlemskap.isINorgeVedFoedselstidspunkt();
        oppgittTilknytningBuilder.medOppholdNå(Boolean.TRUE.equals(iNorgeVedFoedselstidspunkt));

        Objects.requireNonNull(medlemskap, "Medlemskap må være oppgitt");

        settOppholdUtlandPerioder(medlemskap, mottattDato, oppgittTilknytningBuilder);
        settOppholdNorgePerioder(medlemskap, mottattDato, oppgittTilknytningBuilder);
        medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
    }

    private void settOppholdUtlandPerioder(Medlemskap medlemskap,
                                           LocalDate mottattDato,
                                           MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder) {
        medlemskap.getOppholdUtlandet().forEach(opphUtl -> {
            var tidligereOpphold = opphUtl.getPeriode().getFom().isBefore(mottattDato);
            oppgittTilknytningBuilder.leggTilOpphold(byggUtlandsopphold(opphUtl, tidligereOpphold));
        });
    }

    private MedlemskapOppgittLandOppholdEntitet byggUtlandsopphold(OppholdUtlandet utenlandsopphold,
                                                                   boolean tidligereOpphold) {
        return new MedlemskapOppgittLandOppholdEntitet.Builder().medLand(
            finnLandkode(utenlandsopphold.getLand().getKode()))
            .medPeriode(utenlandsopphold.getPeriode().getFom(), utenlandsopphold.getPeriode().getTom())
            .erTidligereOpphold(tidligereOpphold)
            .build();
    }

    private void settOppholdNorgePerioder(Medlemskap medlemskap,
                                          LocalDate mottattDato,
                                          MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder) {
        medlemskap.getOppholdNorge().forEach(opphNorge -> {
            var tidligereOpphold = opphNorge.getPeriode().getFom().isBefore(mottattDato);
            var oppholdNorgeSistePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder().erTidligereOpphold(
                tidligereOpphold)
                .medLand(Landkoder.NOR)
                .medPeriode(opphNorge.getPeriode().getFom(), opphNorge.getPeriode().getTom())
                .build();
            oppgittTilknytningBuilder.leggTilOpphold(oppholdNorgeSistePeriode);
        });
    }

    static Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }

}
