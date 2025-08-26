package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.TerminbekreftelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.SjekkManglendeFødselAksjonspunktDto;
import no.nav.foreldrepenger.familiehendelse.historikk.FødselHistorikkTjeneste;
import no.nav.foreldrepenger.familiehendelse.modell.FødselStatus;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;
import no.nav.vedtak.exception.FunksjonellException;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.format;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SjekkManglendeFødselAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class SjekkManglendeFødselOppdaterer implements AksjonspunktOppdaterer<SjekkManglendeFødselAksjonspunktDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    SjekkManglendeFødselOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public SjekkManglendeFødselOppdaterer(OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                          FamilieHendelseTjeneste familieHendelseTjeneste,
                                          HistorikkinnslagRepository historikkinnslagRepository,
                                          BehandlingRepository behandlingRepository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(SjekkManglendeFødselAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        valider(dto);

        var behandlingId = param.getBehandlingId();
        var grunnlag = familieHendelseTjeneste.hentAggregat(behandlingId);


        if (dto.getBarn() != null) {
            var utledetResultat = utledFødselsdata(dto, grunnlag);
            var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderForOverstyring(behandlingId)
                .tilbakestillBarn()
                .medAntallBarn(utledetResultat.size())
                .medFødselType() // Settes til fødsel for å sikre at typen blir fødsel selv om det ikke er født barn.
                .medErMorForSykVedFødsel(null);
            utledetResultat.forEach(it -> oppdatertOverstyrtHendelse.leggTilBarn(it.getFødselsdato(), it.getDødsdato().orElse(null)));

            familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);
        } else {
            familieHendelseTjeneste.fjernOverstyrtHendelse(behandlingId);
        }

        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());

        opprettHistorikkinnslag(dto, param.getRef(), grunnlag);

        if (Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
            return OppdateringResultat.utenTransisjon().medTotrinn().build();
        } else {
            return OppdateringResultat.utenTransisjon().medTotrinn().medOppdaterGrunnlag().build();
        }
    }

    private void valider(SjekkManglendeFødselAksjonspunktDto dto) {
        if (!dto.getBarn().isEmpty()) {
            if (dto.getBarn().size() > 9) {
                throw new FunksjonellException("FP-076347", "For mange barn", "Oppgi mellom 1 og 9 barn");
            }
            if (dto.getBarn().stream().anyMatch(b -> b.getDødsdato().isPresent() && b.getDødsdato().get().isBefore(b.getFødselsdato()))) {
                throw new FunksjonellException("FP-076345", "Dødsdato før fødselsdato", "Se over fødsels- og dødsdato");
            }
        }
    }

    private List<? extends UidentifisertBarn> utledFødselsdata(SjekkManglendeFødselAksjonspunktDto dto, FamilieHendelseGrunnlagEntitet grunnlag) {
        var termindato = grunnlag.getGjeldendeTerminbekreftelse().map(TerminbekreftelseEntitet::getTermindato);

        var barn = dto.getBarn().stream().map(FødselStatus::new).toList();

        var fødselsdato = barn.stream().map(UidentifisertBarn::getFødselsdato).min(Comparator.naturalOrder());
        if (termindato.isPresent() && fødselsdato.isPresent()) {
            var fødselsintervall = FamilieHendelseTjeneste.intervallForTermindato(termindato.get());
            if (!fødselsintervall.encloses(fødselsdato.get())) {
                throw new FunksjonellException("FP-076346", "For stort avvik termin/fødsel", "Sjekk datoer eller meld sak i Porten");
            }
        }
        return barn;
    }

    private void opprettHistorikkinnslag(SjekkManglendeFødselAksjonspunktDto dto,
                                         BehandlingReferanse behandlingReferanse,
                                         FamilieHendelseGrunnlagEntitet grunnlag) {

        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_FOEDSEL)
            .medFagsakId(behandlingReferanse.fagsakId())
            .medBehandlingId(behandlingReferanse.behandlingId());

        var finnesFødteBarn = !dto.getBarn().isEmpty();
        historikkinnslag.addLinje(new HistorikkinnslagLinjeBuilder().bold("Er barnet født?").tekst(format(finnesFødteBarn)));

        if (finnesFødteBarn) {
            FødselHistorikkTjeneste.lagHistorikkForBarn(historikkinnslag, grunnlag, dto.getBarn());
        }

        historikkinnslag.addLinje(dto.getBegrunnelse());

        historikkinnslagRepository.lagre(historikkinnslag.build());
    }

}
