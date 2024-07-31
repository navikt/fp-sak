package no.nav.foreldrepenger.web.app.tjenester.forvaltning.praksisutsettelse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.aktør.PersoninfoBasis;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakEgenskapRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakLåsRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.egenskaper.FagsakMarkering;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.FpUttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Virkedager;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

/**
 *  Opprette revurderinger der det er oppdaget feil praksis
 */
@ApplicationScoped
public class FeilPraksisOpprettBehandlingTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FeilPraksisOpprettBehandlingTjeneste.class);

    private static final Set<PeriodeResultatÅrsak> FEIL_UTSETTELSE = Set.of(PeriodeResultatÅrsak.HULL_MELLOM_FORELDRENES_PERIODER,
        PeriodeResultatÅrsak.AVSLAG_UTSETTELSE_PGA_FERIE_TILBAKE_I_TID, PeriodeResultatÅrsak.AVSLAG_UTSETTELSE_PGA_ARBEID_TILBAKE_I_TID);

    private BehandlingRepository behandlingRepository;
    private FagsakLåsRepository fagsakLåsRepository;
    private RevurderingTjeneste revurderingTjeneste;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    private PersoninfoAdapter personinfoAdapter;
    private PersonopplysningRepository personopplysningRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private FpUttakRepository fpUttakRepository;
    private FagsakEgenskapRepository fagsakEgenskapRepository;

    @Inject
    public FeilPraksisOpprettBehandlingTjeneste(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                                @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) RevurderingTjeneste revurderingTjeneste,
                                                BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                                BehandlingProsesseringTjeneste behandlingProsesseringTjeneste,
                                                PersoninfoAdapter personinfoAdapter,
                                                PersonopplysningRepository personopplysningRepository,
                                                FamilieHendelseRepository familieHendelseRepository,
                                                FpUttakRepository fpUttakRepository,
                                                FagsakEgenskapRepository fagsakEgenskapRepository) {
        this.fagsakLåsRepository = behandlingRepositoryProvider.getFagsakLåsRepository();
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
        this.revurderingTjeneste = revurderingTjeneste;
        this.personinfoAdapter = personinfoAdapter;
        this.personopplysningRepository = personopplysningRepository;
        this.familieHendelseRepository = familieHendelseRepository;
        this.fpUttakRepository = fpUttakRepository;
        this.fagsakEgenskapRepository = fagsakEgenskapRepository;
    }

    FeilPraksisOpprettBehandlingTjeneste() {
        // CDI
    }

    public void opprettBehandling(Fagsak fagsak) {
        if (harÅpenBehandling(fagsak)) {
            LOG.info("FeilPraksisUtsettelse: Har åpen behandling saksnummer {}", fagsak.getSaksnummer());
            return;
        }

        var sisteVedtatte = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
        if (sisteVedtatte == null) {
            LOG.info("FeilPraksisUtsettelse: Fant ingen vedtatte behandlinger saksnummer {}", fagsak.getSaksnummer());
            return;
        }
        var uttak = fpUttakRepository.hentUttakResultat(sisteVedtatte.getId());
        var tapteDager = tapteDager(uttak.getGjeldendePerioder().getPerioder());
        if (tapteDager.compareTo(Trekkdager.ZERO) == 0) {
            LOG.info("FeilPraksisUtsettelse: Sak gir ikke utslag saksnummer {}", fagsak.getSaksnummer());
            fagsakEgenskapRepository.fjernFagsakMarkering(fagsak.getId(), FagsakMarkering.PRAKSIS_UTSETTELSE);
            return;
        }
        if (tapteDager.compareTo(new Trekkdager(1)) < 0) {
            LOG.info("FeilPraksisUtsettelse: Sak gir for lite utslag saksnummer {} tap {}", fagsak.getSaksnummer(), tapteDager);
            fagsakEgenskapRepository.fjernFagsakMarkering(fagsak.getId(), FagsakMarkering.PRAKSIS_UTSETTELSE);
            return;
        }
        if (harDødsfall(sisteVedtatte)) {
            LOG.info("FeilPraksisUtsettelse: Sak med dødsfall saksnummer {} tap {}", fagsak.getSaksnummer(), tapteDager);
            return;
        }
        if (famileHendelseEtterPraksisendring(sisteVedtatte)) {
            LOG.info("FeilPraksisUtsettelse: Barn født etter lovendring saksnummer {} tap {}", fagsak.getSaksnummer(), tapteDager);
            fagsakEgenskapRepository.fjernFagsakMarkering(fagsak.getId(), FagsakMarkering.PRAKSIS_UTSETTELSE);
            return;
        }

        var enhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFra(sisteVedtatte);
        fagsakLåsRepository.taLås(fagsak.getId());
        var revurdering = revurderingTjeneste.opprettAutomatiskRevurdering(fagsak, BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE, enhet);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(revurdering);
        LOG.info("FeilPraksisUtsettelse: Opprettet revurdering med behandlingId {} saksnummer {}", revurdering.getId(), fagsak.getSaksnummer());

    }

    private static Trekkdager tapteDager(List<UttakResultatPeriodeEntitet> perioder) {
        Map<UttakAktivitetGruppering, Trekkdager> trekkdagerPrAktivitet = new LinkedHashMap<>(tapteDagerUtsettelse(perioder));
        tapteDagerGradering(perioder).forEach((k,v) -> trekkdagerPrAktivitet.put(k, trekkdagerPrAktivitet.getOrDefault(k, Trekkdager.ZERO).add(v)));
        return trekkdagerPrAktivitet.values().stream().max(Comparator.naturalOrder()).orElse(Trekkdager.ZERO);
    }

    private static Map<UttakAktivitetGruppering, Trekkdager> tapteDagerUtsettelse(List<UttakResultatPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(p -> FEIL_UTSETTELSE.contains(p.getResultatÅrsak()))
            .map(FeilPraksisOpprettBehandlingTjeneste::tapteDagerUtsettelsePeriode)
            .flatMap(Collection::stream)
            .collect(Collectors.groupingBy(UttakAktivitetTapteDager::grupperingsnøkkel,
                Collectors.reducing(Trekkdager.ZERO, UttakAktivitetTapteDager::taptedager, Trekkdager::add)));
    }

    private static List<UttakAktivitetTapteDager> tapteDagerUtsettelsePeriode(UttakResultatPeriodeEntitet periode) {
        return periode.getAktiviteter().stream()
            .filter(a -> a.getTrekkdager().merEnn0() && !a.getUtbetalingsgrad().harUtbetaling())
            .map(a -> new UttakAktivitetTapteDager(new UttakAktivitetGruppering(a), a.getTrekkdager()))
            .toList();
    }

    private static Map<UttakAktivitetGruppering, Trekkdager> tapteDagerGradering(List<UttakResultatPeriodeEntitet> perioder) {
        return perioder.stream()
            .filter(p -> GraderingAvslagÅrsak.FOR_SEN_SØKNAD.equals(p.getGraderingAvslagÅrsak()) && p.getSamtidigUttaksprosent() == null)
            .map(FeilPraksisOpprettBehandlingTjeneste::tapteDagerGraderingPeriode)
            .flatMap(Collection::stream)
            .collect(Collectors.groupingBy(UttakAktivitetTapteDager::grupperingsnøkkel,
                Collectors.reducing(Trekkdager.ZERO, UttakAktivitetTapteDager::taptedager, Trekkdager::add)));
    }

    private static List<UttakAktivitetTapteDager> tapteDagerGraderingPeriode(UttakResultatPeriodeEntitet periode) {
        return periode.getAktiviteter().stream()
            .filter(a -> a.getUtbetalingsgrad().compareTo(Utbetalingsgrad.HUNDRED) < 0 && feilPraksisGraderingAktivitet(periode, a))
            .map(a -> new UttakAktivitetTapteDager(new UttakAktivitetGruppering(a),
                a.getTrekkdager().subtract(graderingForventetTrekkdager(periode, a))))
            .toList();
    }


    private static boolean feilPraksisGraderingAktivitet(UttakResultatPeriodeEntitet periode, UttakResultatPeriodeAktivitetEntitet aktivitet) {
        var forventetTrekkdager = graderingForventetTrekkdager(periode, aktivitet);
        return aktivitet.getTrekkdager().compareTo(forventetTrekkdager) > 0;
    }

    private static Trekkdager graderingForventetTrekkdager(UttakResultatPeriodeEntitet periode, UttakResultatPeriodeAktivitetEntitet aktivitet) {
        var virkedager = Virkedager.beregnAntallVirkedager(periode.getFom(), periode.getTom());
        var forventetTrekkdager = new BigDecimal(virkedager).multiply(aktivitet.getUtbetalingsgrad().decimalValue()).divide(BigDecimal.valueOf(100L), 1, RoundingMode.DOWN);
        return new Trekkdager(forventetTrekkdager);
    }

    private boolean harÅpenBehandling(Fagsak fagsak) {
        return !behandlingRepository.hentÅpneBehandlingerIdForFagsakId(fagsak.getId()).isEmpty();
    }

    private boolean famileHendelseEtterPraksisendring(Behandling behandling) {
        var hendelseEtterPraksisdato = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
            .filter(stp -> stp.isAfter(UtsettelseCore2021.IKRAFT_FRA_DATO.minusDays(1)));

        return hendelseEtterPraksisdato.isPresent();
    }

    private boolean harDødsfall(Behandling behandling) {
        var barnDødsdato = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getBarna).orElse(List.of())
            .stream().anyMatch(b -> b.getDødsdato().isPresent());
        var personDødsdato = personopplysningRepository.hentPersonopplysningerHvisEksisterer(behandling.getId())
            .map(PersonopplysningGrunnlagEntitet::getGjeldendeVersjon)
            .map(PersonInformasjonEntitet::getPersonopplysninger).orElse(List.of())
            .stream().anyMatch(p -> p.getDødsdato() != null || harDødsdato(p.getAktørId()));

        return barnDødsdato || personDødsdato;
    }

    private boolean harDødsdato(AktørId aktørId) {
        return personinfoAdapter.hentBrukerBasisForAktør(FagsakYtelseType.FORELDREPENGER, aktørId)
            .map(PersoninfoBasis::dødsdato).isPresent();
    }

    private record UttakAktivitetGruppering(UttakArbeidType uttakArbeidType, Arbeidsgiver arbeidsgiver, InternArbeidsforholdRef ref) {
        UttakAktivitetGruppering(UttakResultatPeriodeAktivitetEntitet aktivitet) {
            this(aktivitet.getUttakArbeidType(), aktivitet.getArbeidsgiver(), aktivitet.getArbeidsforholdRef());
        }
    }

    private record UttakAktivitetTapteDager(UttakAktivitetGruppering grupperingsnøkkel, Trekkdager taptedager) { }

}
