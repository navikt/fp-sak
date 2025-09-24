package no.nav.foreldrepenger.familiehendelse.aksjonspunkt.adopsjon;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.fraTilEquals;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarn;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.UidentifisertBarnEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.adopsjon.dto.BekreftDokumentertDatoAksjonspunktDto;
import no.nav.foreldrepenger.skjæringstidspunkt.OpplysningsPeriodeTjeneste;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftDokumentertDatoAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftDokumentasjonOppdaterer implements AksjonspunktOppdaterer<BekreftDokumentertDatoAksjonspunktDto> {

    private FamilieHendelseTjeneste familieHendelseTjeneste;
    private OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste;
    private HistorikkinnslagRepository historikkinnslagRepository;

    BekreftDokumentasjonOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftDokumentasjonOppdaterer(FamilieHendelseTjeneste familieHendelseTjeneste,
                                          OpplysningsPeriodeTjeneste opplysningsPeriodeTjeneste,
                                          HistorikkinnslagRepository historikkinnslagRepository) {
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        this.opplysningsPeriodeTjeneste = opplysningsPeriodeTjeneste;
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(BekreftDokumentertDatoAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();
        var forrigeFikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        var hendelseGrunnlag = familieHendelseTjeneste.hentAggregat(param.getBehandlingId());

        var originalOmsorgsovertakelse = getOmsorgsovertakelsesdatoForAdopsjon(hendelseGrunnlag.getGjeldendeAdopsjon().orElseThrow());
        var originalFødselsdatoer = getAdopsjonFødselsdatoer(hendelseGrunnlag);
        var toTrinn = !Objects.equals(originalOmsorgsovertakelse, dto.getOmsorgsovertakelseDato()) || !Objects.equals(originalFødselsdatoer,
            dto.getFodselsdatoer());
        var erEndret = toTrinn || param.erBegrunnelseEndret();
        if (erEndret) {
            lagreHistorikk(param, dto, originalOmsorgsovertakelse, originalFødselsdatoer);
        }

        var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderForOverstyring(behandlingId);
        oppdatertOverstyrtHendelse.tilbakestillBarn()
            .medAntallBarn(dto.getFodselsdatoer().keySet().size())
            .medAdopsjon(oppdatertOverstyrtHendelse.getAdopsjonBuilder().medOmsorgsovertakelseDato(dto.getOmsorgsovertakelseDato()));
        dto.getFodselsdatoer()
            .forEach((barnnummer, fødselsdato) -> oppdatertOverstyrtHendelse.leggTilBarn(new UidentifisertBarnEntitet(fødselsdato, barnnummer)));

        familieHendelseTjeneste.lagreOverstyrtHendelse(behandlingId, oppdatertOverstyrtHendelse);

        var sistefikspunkt = opplysningsPeriodeTjeneste.utledFikspunktForRegisterInnhenting(behandlingId, param.getRef().fagsakYtelseType());
        if (Objects.equals(forrigeFikspunkt, sistefikspunkt)) {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(toTrinn).build();
        } else {
            return OppdateringResultat.utenTransisjon().medTotrinnHvis(toTrinn).medOppdaterGrunnlag().build();
        }
    }

    private void lagreHistorikk(AksjonspunktOppdaterParameter param,
                                BekreftDokumentertDatoAksjonspunktDto dto,
                                LocalDate originalOmsorgsovertakelse,
                                Map<Integer, LocalDate> originaleFødselsdatoer) {


        var builder = new Historikkinnslag.Builder().medFagsakId(param.getFagsakId())
            .medBehandlingId(param.getBehandlingId())
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medTittel(SkjermlenkeType.FAKTA_OM_ADOPSJON)
            .addLinje(fraTilEquals("Omsorgsovertakelsesdato", originalOmsorgsovertakelse, dto.getOmsorgsovertakelseDato()));

        for (Map.Entry<Integer, LocalDate> eksisterendeFødselsdatoer : originaleFødselsdatoer.entrySet()) {
            var eksisterende = eksisterendeFødselsdatoer.getValue();
            var oppdatert = dto.getFodselsdatoer().get(eksisterendeFødselsdatoer.getKey());
            builder.addLinje(fraTilEquals("Fødselsdato", eksisterende, oppdatert));
        }

        var historikkinnslag = builder.addLinje(dto.getBegrunnelse()).build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }

    private LocalDate getOmsorgsovertakelsesdatoForAdopsjon(AdopsjonEntitet adopsjon) {
        return adopsjon.getOmsorgsovertakelseDato();
    }

    private Map<Integer, LocalDate> getAdopsjonFødselsdatoer(FamilieHendelseGrunnlagEntitet grunnlag) {
        return Optional.ofNullable(grunnlag.getGjeldendeBarna())
            .map(barna -> barna.stream().collect(toMap(UidentifisertBarn::getBarnNummer, UidentifisertBarn::getFødselsdato)))
            .orElse(emptyMap());
    }
}
