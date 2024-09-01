package no.nav.foreldrepenger.domene.medlem.impl;

import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.JA;
import static no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall.NEI;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

public class AvklaringFaktaMedlemskap {

    private SøknadRepository søknadRepository;
    private MedlemskapRepository medlemskapRepository;
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public AvklaringFaktaMedlemskap(BehandlingRepositoryProvider repositoryProvider,
                                    MedlemskapPerioderTjeneste medlemskapPerioderTjeneste,
                                    PersonopplysningTjeneste personopplysningTjeneste,
                                    InntektArbeidYtelseTjeneste iayTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapPerioderTjeneste = medlemskapPerioderTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
    }

    public Optional<MedlemResultat> utled(BehandlingReferanse ref, Skjæringstidspunkt stp, Behandling behandling, LocalDate vurderingsdato) {
        var behandlingId = behandling.getId();
        var medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);

        Set<MedlemskapPerioderEntitet> medlemskapPerioder = medlemskap.isPresent()
            ? medlemskap.get().getRegistrertMedlemskapPerioder()
            : Collections.emptySet();

        var personopplysninger = personopplysningTjeneste.hentPersonopplysninger(ref);

        if (harDekningsgrad(vurderingsdato, medlemskapPerioder) == JA) {
            if (erFrivilligMedlem(vurderingsdato, medlemskapPerioder) == JA) {
                return Optional.empty();
            }
            if (erUnntatt(vurderingsdato, medlemskapPerioder) == JA) {
                if (harStatsborgerskapUSAellerPNG(personopplysninger, stp.getUtledetSkjæringstidspunkt()) == JA) {
                    if (harStatusUtvandret(personopplysninger, vurderingsdato) == JA) {
                        return Optional.empty();
                    }
                    return Optional.of(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD);
                }
                return Optional.empty();
            }
            if (erIkkeMedlem(vurderingsdato, medlemskapPerioder) == JA) {
                return Optional.empty();
            }
        } else if (erUavklart(vurderingsdato, medlemskapPerioder) == JA) {
            return Optional.of(MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
        } else {
            if (harStatusUtvandret(personopplysninger, vurderingsdato) == JA) {
                return Optional.empty();
            }
            if (harOppholdstilltatelseVed(ref, stp, vurderingsdato) == JA) {
                return Optional.empty();
            }
            var erEtterSkjæringstidspunkt = vurderingsdato.isAfter(stp.getUtledetSkjæringstidspunkt());
            var region = statsborgerskap(personopplysninger, vurderingsdato, stp.getUtledetSkjæringstidspunkt());
            return switch (region) {
                case EØS -> harInntektSiste3mnd(ref, vurderingsdato) == JA || erEtterSkjæringstidspunkt ? Optional.empty() : Optional.of(MedlemResultat.AVKLAR_OPPHOLDSRETT);
                case TREDJE_LANDS_BORGER -> Optional.of(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD);
                case NORDISK -> Optional.empty();
            };
        }
        throw new IllegalStateException("Udefinert utledning av aksjonspunkt for medlemskapsfakta");
    }

    Statsborgerskapsregioner statsborgerskap(PersonopplysningerAggregat søker, LocalDate vurderingsdato, LocalDate skjæringstidspunkt) {
        var region = søker.getStatsborgerskapRegionVedTidspunkt(søker.getSøker().getAktørId(), vurderingsdato, skjæringstidspunkt);
        return switch (region) {
            case NORDEN -> Statsborgerskapsregioner.NORDISK;
            case EOS -> Statsborgerskapsregioner.EØS;
            default -> Statsborgerskapsregioner.TREDJE_LANDS_BORGER;
        };
    }

    private Utfall harOppholdstilltatelseVed(BehandlingReferanse ref, Skjæringstidspunkt stp, LocalDate vurderingsdato) {
        if (stp.getUttaksintervall().filter(i -> i.encloses(vurderingsdato)).isPresent()) {
            return personopplysningTjeneste.harOppholdstillatelseForPeriode(ref.behandlingId(), stp.getUttaksintervall().orElseThrow()) ? JA : NEI;
        }
        return personopplysningTjeneste.harOppholdstillatelsePåDato(ref.behandlingId(), vurderingsdato) ? JA : NEI;
    }

    private Utfall harDekningsgrad(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        var medlemskapDekningTypes = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder,
            vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomAvklartMedlemskap(medlemskapDekningTypes) ? JA : NEI;
    }

    private Utfall erFrivilligMedlem(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        var dekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder, vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomFrivilligMedlem(dekningTyper) ? JA : NEI;
    }

    private Utfall erUnntatt(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        var dekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder, vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomUnntatt(dekningTyper) ? JA : NEI;
    }

    private Utfall erIkkeMedlem(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        var dekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder, vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomIkkeMedlem(dekningTyper) ? JA : NEI;
    }

    private Utfall erUavklart(LocalDate vurderingsdato, Set<MedlemskapPerioderEntitet> medlemskapPerioder) {
        var medlemskapDekningTyper = medlemskapPerioderTjeneste.finnGyldigeDekningstyper(medlemskapPerioder,
            vurderingsdato);
        return medlemskapPerioderTjeneste.erRegistrertSomUavklartMedlemskap(medlemskapDekningTyper) ? JA : NEI;
    }

    private Utfall harStatusUtvandret(PersonopplysningerAggregat bruker, LocalDate vurderingsdato) {
        return medlemskapPerioderTjeneste.erStatusUtvandret(bruker, vurderingsdato) ? JA : NEI;
    }

    private Utfall harStatsborgerskapUSAellerPNG(PersonopplysningerAggregat bruker, LocalDate skjæringstidspunkt) {
        return medlemskapPerioderTjeneste.harStatsborgerskapUsaEllerPng(bruker, skjæringstidspunkt) ? JA : NEI;
    }

    /**
     * Skal sjekke om bruker eller andre foreldre har inntekt eller ytelse fra NAV
     * innenfor de 3 siste månedene fra mottattdato
     */
    private Utfall harInntektSiste3mnd(BehandlingReferanse ref, LocalDate vurderingsdato) {
        var siste3Mnd = utledInntektsintervall3Mnd(ref, vurderingsdato);
        var grunnlag = iayTjeneste.finnGrunnlag(ref.behandlingId());

        var inntektSiste3M = false;
        if (grunnlag.isPresent()) {
            var filter = new InntektFilter(grunnlag.get().getAktørInntektFraRegister(ref.aktørId())).før(vurderingsdato);
            inntektSiste3M = filter.getInntektsposterPensjonsgivende().stream()
                .anyMatch(ip -> siste3Mnd.overlapper(ip.getPeriode()));
        }
        return inntektSiste3M ? JA : NEI;
    }

    private DatoIntervallEntitet utledInntektsintervall3Mnd(BehandlingReferanse referanse, LocalDate vurderingstidspunkt) {
        if (FagsakYtelseType.ENGANGSTØNAD.equals(referanse.fagsakYtelseType()) && LocalDate.now().isBefore(vurderingstidspunkt)) {
            var søknadMottattDato = søknadRepository.hentSøknad(referanse.behandlingId()).getMottattDato();
            var brukdato = søknadMottattDato.isBefore(vurderingstidspunkt) ? søknadMottattDato : vurderingstidspunkt;
            return DatoIntervallEntitet.fraOgMedTilOgMed(brukdato.minusMonths(3), brukdato);
        }
        return DatoIntervallEntitet.fraOgMedTilOgMed(vurderingstidspunkt.minusMonths(3), vurderingstidspunkt);
    }

    enum Statsborgerskapsregioner {
        NORDISK, EØS, TREDJE_LANDS_BORGER
    }
}
