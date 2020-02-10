package no.nav.foreldrepenger.domene.risikoklassifisering.json;

import static no.nav.vedtak.feil.LogLevel.WARN;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.risikoklassifisering.modell.Kontrollresultat;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalGruppeWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.FaresignalWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.KontrollresultatWrapper;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest.FaresignalerRespons;
import no.nav.foreldrepenger.domene.risikoklassifisering.tjeneste.dto.rest.Faresignalgruppe;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;
import no.nav.vedtak.kontroll.v1.KontrollResultatV1;

@ApplicationScoped
public class KontrollresultatMapper {

    @Inject
    public KontrollresultatMapper() {
    }

    public KontrollresultatWrapper fraKontrakt(KontrollResultatV1 kontraktResultat) {
        if (kontraktResultat.getKontrollResultatkode() == null || kontraktResultat.getKontrollResultatkode().getKode() == null) {
            throw Feilene.FACTORY.manglerKontrollresultatkode().toException();
        }
        String kode = kontraktResultat.getKontrollResultatkode().getKode();
        Kontrollresultat kontrollresultat = finnKontrollresultat(kode);
        return new KontrollresultatWrapper(kontraktResultat.getBehandlingUuid(), kontrollresultat);
    }

    private Kontrollresultat finnKontrollresultat(String kode) {
        if (kode == null) {
            return null;
        }
        Kontrollresultat kontrollresultat = Kontrollresultat.fraKode(kode);
        if (kontrollresultat == null || Kontrollresultat.UDEFINERT.equals(kontrollresultat)) {
            throw Feilene.FACTORY.udefinertKontrollresultat().toException();
        }
        return kontrollresultat;
    }

    public FaresignalWrapper fraFaresignalRespons(FaresignalerRespons faresignalerRespons) {
        if (faresignalerRespons.getRisikoklasse() == null) {
            // Her ønsker vi ikke akseptere at risikoklasse er null
            throw Feilene.FACTORY.manglerKontrollresultatkode().toException();
        }
        return FaresignalWrapper.builder()
            .medKontrollresultat(finnKontrollresultat(faresignalerRespons.getRisikoklasse()))
            .medMedlFaresignaler(mapFaresignalgruppe(faresignalerRespons.getMedlFaresignaler()).orElse(null))
            .medIayFaresignaler(mapFaresignalgruppe(faresignalerRespons.getIayFaresignaler()).orElse(null))
            .build();
    }

    private Optional<FaresignalGruppeWrapper> mapFaresignalgruppe(Faresignalgruppe faresignalGruppe) {
        if (faresignalGruppe == null || faresignalGruppe.getFaresignaler().isEmpty()) {
            return Optional.empty();
        }
        FaresignalGruppeWrapper.Builder builder = FaresignalGruppeWrapper.builder()
            .medKontrollresultat(finnKontrollresultat(faresignalGruppe.getRisikoklasse()));
        faresignalGruppe.getFaresignaler().forEach(builder::leggTilFaresignal);
        return Optional.of(builder.build());
    }

    interface Feilene extends DeklarerteFeil {
        KontrollresultatMapper.Feilene FACTORY = FeilFactory.create(KontrollresultatMapper.Feilene.class);

        @TekniskFeil(feilkode = "FP-42517", feilmelding = "Mangler kontrollresultatkode på kontrollresultat", logLevel = WARN)
        Feil manglerKontrollresultatkode();

        @TekniskFeil(feilkode = "FP-42518", feilmelding = "Udefinert kontrollresultat", logLevel = WARN)
        Feil udefinertKontrollresultat();
    }

}
