package no.nav.foreldrepenger.domene.medlem.impl;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;


public class AvklarBarnFødtUtenlands {

    private MedlemskapRepository medlemskapRepository;
    private FamilieHendelseRepository familieHendelseRepository;

    public AvklarBarnFødtUtenlands(BehandlingRepositoryProvider repositoryProvider) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    //NOSONAR
    public Optional<MedlemResultat> utled(Long behandlingId, @SuppressWarnings("unused") LocalDate vurderingsdato) {
        var bekreftetFH = familieHendelseRepository.hentAggregat(behandlingId).getBekreftetVersjon().orElse(null);

        if (!((erSøktPåBakgrunnAvFødselsdato(behandlingId) == JA) || erFødselBekreftet(bekreftetFH) == JA)) {
            return Optional.empty();
        }
        if ((erFødselsdatoFraTpsInnenforEnOppgittUtlandsperiode(bekreftetFH, behandlingId) == JA)
            || (erFødselsdatoFraSøknadInnenforEnOppgittUtlandsperiode(behandlingId) == JA)) {
            return Optional.of(MedlemResultat.AVKLAR_OM_ER_BOSATT);
        }
        return Optional.empty();
    }

    private Utfall erSøktPåBakgrunnAvFødselsdato(Long behandlingId) {
        var grunnlag = familieHendelseRepository.hentAggregat(behandlingId);

        if (!grunnlag.getGjeldendeVersjon().getTerminbekreftelse().isPresent() && !grunnlag.getGjeldendeVersjon().getAdopsjon().isPresent()) {
            return JA;
        }
        return NEI;
    }

    private Utfall erFødselBekreftet(FamilieHendelseEntitet bekreftet) {
        return bekreftet != null && ! bekreftet.getBarna().isEmpty() ? JA : NEI;
    }

    private Utfall erFødselsdatoFraTpsInnenforEnOppgittUtlandsperiode(FamilieHendelseEntitet bekreftet, Long behandlingId) {
        var aggregat = medlemskapRepository.hentMedlemskap(behandlingId);
        if (!aggregat.isPresent() || bekreftet == null) {
            return NEI;
        }
        var medlemskapAggregat = aggregat.get();
        var utenlandsopphold = getOppgittUtenlandsOpphold(medlemskapAggregat);
        if (!utenlandsopphold.isPresent()) {
            return NEI;
        }

        for (var barnet : bekreftet.getBarna()) {
            if (erFødselsdatoInnenforEtUtenlandsopphold(barnet.getFødselsdato(), utenlandsopphold)) {
                return JA;
            }
        }
        return NEI;
    }

    private Utfall erFødselsdatoFraSøknadInnenforEnOppgittUtlandsperiode(Long behandlingId) {
        var grunnlag = familieHendelseRepository.hentAggregat(behandlingId);

        var aggregat = medlemskapRepository.hentMedlemskap(behandlingId);
        if (!aggregat.isPresent()) {
            return NEI;
        }
        var medlemskapAggregat = aggregat.get();
        var utenlandsopphold = getOppgittUtenlandsOpphold(medlemskapAggregat);
        if (!utenlandsopphold.isPresent()) {
            return NEI;
        }
        var barnFraSøknad = grunnlag.getGjeldendeVersjon().getBarna();

        for (var barnet : barnFraSøknad) {
            if (erFødselsdatoInnenforEtUtenlandsopphold(barnet.getFødselsdato(), utenlandsopphold)) {
                return JA;
            }
        }
        return NEI;
    }

    private Optional<Set<MedlemskapOppgittLandOppholdEntitet>> getOppgittUtenlandsOpphold(MedlemskapAggregat medlemskapAggregat) {
        var opphold = medlemskapAggregat.getOppgittTilknytning().map(MedlemskapOppgittTilknytningEntitet::getOpphold);
        if (!opphold.isPresent()) {
            return Optional.empty();
        }
        var utenlandsOpphold = opphold.get().stream().filter(o -> !o.getLand().equals(Landkoder.NOR)).collect(Collectors.toSet());
        if (utenlandsOpphold.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(utenlandsOpphold);
    }

    private boolean erFødselsdatoInnenforEtUtenlandsopphold(LocalDate barnetsFødselsdato, Optional<Set<MedlemskapOppgittLandOppholdEntitet>> utenlandsopphold) {
        for (var utenlandsoppholdet : utenlandsopphold.get()) {
            if (erBarnetFødtUnderDetteUtenlandsoppholdet(barnetsFødselsdato, utenlandsoppholdet.getPeriodeFom(), utenlandsoppholdet.getPeriodeTom())) {
                return true;
            }
        }
        return false;
    }

    private boolean erBarnetFødtUnderDetteUtenlandsoppholdet(LocalDate barnetsFødselsdato, LocalDate startUtenlandsopphold, LocalDate sluttUtenlandsopphold) {
        return barnetsFødselsdato.isAfter(startUtenlandsopphold) && barnetsFødselsdato.isBefore(sluttUtenlandsopphold);
    }
}
