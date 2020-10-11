package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.adapter;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingInntrekkEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilbakekreving.TilbakekrevingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.FamilieYtelseType;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdrag110;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.PersonIdent;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.økonomi.økonomistøtte.HentOppdragMedPositivKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.SjekkOmDetFinnesTilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.fp.OppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.BehandlingVedtakOppdrag;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.ForrigeOppdragInput;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelse;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.TilkjentYtelsePeriode;

@ApplicationScoped
public class BehandlingTilOppdragMapperTjeneste {

    private BeregningsresultatRepository beregningsresultatRepository;
    private HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering;
    private MapBehandlingVedtak mapBehandlingVedtak;
    private PersoninfoAdapter personinfoAdapter;
    private FamilieHendelseRepository familieHendelseRepository;
    private TilbakekrevingRepository tilbakekrevingRepository;
    private SjekkOmDetFinnesTilkjentYtelse sjekkOmDetFinnesTilkjentYtelse;

    BehandlingTilOppdragMapperTjeneste() {
        // for CDI proxy
    }

    @Inject
    public BehandlingTilOppdragMapperTjeneste(HentOppdragMedPositivKvittering hentOppdragMedPositivKvittering,
                                              MapBehandlingVedtak mapBehandlingVedtak,
                                              PersoninfoAdapter personinfoAdapter,
                                              TilbakekrevingRepository tilbakekrevingRepository,
                                              BeregningsresultatRepository beregningsresultatRepository,
                                              FamilieHendelseRepository familieHendelseRepository,
                                              SjekkOmDetFinnesTilkjentYtelse sjekkOmDetFinnesTilkjentYtelse) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.hentOppdragMedPositivKvittering = hentOppdragMedPositivKvittering;
        this.mapBehandlingVedtak = mapBehandlingVedtak;
        this.personinfoAdapter = personinfoAdapter;
        this.familieHendelseRepository = familieHendelseRepository;
        this.tilbakekrevingRepository = tilbakekrevingRepository;
        this.sjekkOmDetFinnesTilkjentYtelse = sjekkOmDetFinnesTilkjentYtelse;
    }

    public OppdragInput map(Behandling behandling) {
        // kallet kan fjernes en gang i fremtiden, når Oppdragssystemet ikke lenger krever fnr i sine meldinger.
        PersonIdent personIdent = personinfoAdapter.hentFnrForAktør(behandling.getAktørId());
        Long behandlingId = behandling.getId();
        TilkjentYtelseMapper tilkjentYtelseMapper = new TilkjentYtelseMapper(finnFamilieYtelseType(behandling));
        TilkjentYtelse tilkjentYtelse = null;
        List<TilkjentYtelsePeriode> tilkjentYtelsePerioderFomEndringsdato = Collections.emptyList();
        boolean opphørEtterStpEllerIkkeOpphør = erOpphørEtterSkjæringstidspunktEllerIkkeOpphør(behandling);
        if (opphørEtterStpEllerIkkeOpphør) {
            BeregningsresultatEntitet beregningsresultat = beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId)
                .orElseThrow(() -> new IllegalStateException("Mangler Beregningsresultat for behandling " + behandlingId));
            tilkjentYtelse = tilkjentYtelseMapper.map(beregningsresultat);
            tilkjentYtelsePerioderFomEndringsdato = tilkjentYtelseMapper.mapPerioderFomEndringsdato(beregningsresultat);
        }
        BehandlingVedtakOppdrag behandlingVedtakFP = mapBehandlingVedtak.map(behandling);
        ForrigeOppdragInput forrigeOppdragInput = lagTidligereBehandlingInfo(behandling, tilkjentYtelseMapper);

        Optional<TilbakekrevingInntrekkEntitet> tilbakekrevingInntrekk = tilbakekrevingRepository.hentTilbakekrevingInntrekk(behandling.getId());
        boolean avslåttInntrekk = tilbakekrevingInntrekk.map(TilbakekrevingInntrekkEntitet::isAvslåttInntrekk).orElse(false);

        return OppdragInput.builder()
            .medBehandlingId(behandling.getId())
            .medSaksnummer(behandling.getFagsak().getSaksnummer())
            .medFagsakYtelseType(behandling.getFagsakYtelseType())
            .medBehandlingVedtak(behandlingVedtakFP)
            .medPersonIdent(personIdent)
            .medForenkletBeregningsresultat(tilkjentYtelse)
            .medTilkjentYtelsePerioderFomEndringsdato(tilkjentYtelsePerioderFomEndringsdato)
            .medTidligereBehandlingInfo(forrigeOppdragInput)
            .medOpphørEtterStpEllerIkkeOpphør(opphørEtterStpEllerIkkeOpphør)
            .medAvslåttInntrekk(avslåttInntrekk)
            .build();
    }

    private FamilieYtelseType finnFamilieYtelseType(Behandling behandling) {
        FagsakYtelseType fagsakYtelseType = behandling.getFagsakYtelseType();
        if (FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)) {
            return gjelderFødsel(behandling.getId())
                ? FamilieYtelseType.FØDSEL
                : FamilieYtelseType.ADOPSJON;
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsakYtelseType)) {
            return FamilieYtelseType.SVANGERSKAPSPENGER;
        } else {
            throw new IllegalArgumentException("Utvikler feil: Ikke støttet fagsak ytelse type: " + fagsakYtelseType);
        }
    }

    private ForrigeOppdragInput lagTidligereBehandlingInfo(Behandling behandling, TilkjentYtelseMapper mapper) {
        Optional<BeregningsresultatEntitet> forrigeBeregningsresultatFPOpt = getForrigeBeregningsresultatFP(behandling);
        TilkjentYtelse forrigeTilkjentYtelse = null;
        if (forrigeBeregningsresultatFPOpt.isPresent()) {
            forrigeTilkjentYtelse = mapper.map(forrigeBeregningsresultatFPOpt.get());
        }
        Saksnummer saksnummer = behandling.getFagsak().getSaksnummer();
        List<Oppdrag110> tidligereOppdrag110 = hentOppdragMedPositivKvittering.hentOppdragMedPositivKvittering(saksnummer);
        return new ForrigeOppdragInput(tidligereOppdrag110, forrigeTilkjentYtelse);
    }

    private Optional<BeregningsresultatEntitet> getForrigeBeregningsresultatFP(Behandling behandling) {
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            return Optional.empty();
        }
        Long originalBehandlingId = behandling.getOriginalBehandlingId()
            .orElseThrow(() -> new IllegalStateException("Utvikler feil: Revurdering uten original behandling"));
        return beregningsresultatRepository.hentUtbetBeregningsresultat(originalBehandlingId);
    }

    private boolean gjelderFødsel(Long behandlingId) {
        return familieHendelseRepository.hentAggregat(behandlingId)
            .getGjeldendeVersjon().getGjelderFødsel();
    }

    private boolean erOpphørEtterSkjæringstidspunktEllerIkkeOpphør(Behandling behandling) {
        boolean erOpphør = behandling.getBehandlingsresultat().isBehandlingsresultatOpphørt();
        boolean erOpphørEtterStp = sjekkOmDetFinnesTilkjentYtelse.harTilkjentYtelse(behandling.getId());
        return !erOpphør || erOpphørEtterStp;
    }
}
